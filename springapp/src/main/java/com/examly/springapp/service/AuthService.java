package com.examly.springapp.service;

import com.examly.springapp.dto.AuthDto.*;
import com.examly.springapp.exception.*;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.security.JwtUtil;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest req) {
        if (req.getName() == null || req.getName().trim().isEmpty())
            throw new InvalidNameException("Name is required");
        if (!req.getName().matches("^[a-zA-Z ]+$"))
            throw new InvalidNameException("Name must not contain numbers or special characters");
        if (req.getPhone() == null || req.getPhone().trim().isEmpty())
            throw new InvalidPhoneException("Phone Number is required");
        if (!req.getPhone().matches("^\\d{10}$"))
            throw new InvalidPhoneException("Phone Number must be exactly 10 digits long");
        if (req.getEmail() == null || !req.getEmail().contains("@"))
            throw new IllegalArgumentException("Please enter a valid email address");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new DuplicateTransactionException("This email is already registered");
        if (req.getPassword() == null || req.getPassword().length() < 8)
            throw new IllegalArgumentException("Password must meet security requirements");

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole() != null ? User.Role.valueOf(req.getRole().toUpperCase()) : User.Role.USER);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getName());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getName());
    }
}

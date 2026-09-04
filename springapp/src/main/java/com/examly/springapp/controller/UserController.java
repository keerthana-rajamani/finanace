package com.examly.springapp.controller;

import com.examly.springapp.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/users", "/api/user"})
public class UserController {

    private final AuthHelper authHelper;

    public UserController(AuthHelper authHelper) {
        this.authHelper = authHelper;
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile() {
        User user = authHelper.getCurrentUser();
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}

package com.examly.springapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "Name must not contain numbers or special characters")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Email(message = "Please enter a valid email address")
    @Column(unique = true)
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\d{10}$", message = "Phone Number must be exactly 10 digits long")
    private String phone;

    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    public enum Role {
        USER, FAMILY_MEMBER, FINANCIAL_ADVISOR, SUPPORT_AGENT, ADMIN
    }
}

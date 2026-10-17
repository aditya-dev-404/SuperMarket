package com.aditya.supermarket.user.dto;

import com.aditya.supermarket.user.entity.Role;
import com.aditya.supermarket.user.entity.User;
import java.time.Instant;

/** Public representation of a user. Never contains the password hash. */
public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt());
    }
}

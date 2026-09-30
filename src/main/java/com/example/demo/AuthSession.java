package com.example.demo;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

// Вошедший сотрудник. Хранится в памяти сервера (SessionStore) по токену.
public record AuthSession(
        String token,
        Long employeeId,
        String name,
        Employee.Role role,
        @JsonIgnore Long workSessionId,
        Instant expiresAt) {

    @JsonIgnore
    public boolean isDirector() {
        return role == Employee.Role.DIRECTOR;
    }
}

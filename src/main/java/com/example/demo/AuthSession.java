package com.example.demo;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

// Вошедший пользователь. Хранится в памяти сервера (SessionStore) по токену.
// role = EMPLOYEE — сотрудник (доступ ко всему), CITIZEN — гражданин (только свой лицевой счёт).
public record AuthSession(
        String token,
        Role role,
        String name,
        Long subscriberId,      // только для гражданина
        String serviceType,     // только для гражданина
        Instant expiresAt) {

    public enum Role { EMPLOYEE, CITIZEN }

    @JsonIgnore
    public boolean isEmployee() {
        return role == Role.EMPLOYEE;
    }
}

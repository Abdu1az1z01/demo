package com.example.demo;

import java.time.Instant;

// Вошедший сотрудник инспекции. Хранится в памяти сервера (SessionStore) по токену.
public record AuthSession(
        String token,
        String name,
        Instant expiresAt) {
}

package com.example.demo;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// Токены входа. Хранятся в памяти: после перезапуска бэкенда нужно войти заново.
@Component
public class SessionStore {

    // Сколько действует вход
    private static final Duration LIFETIME = Duration.ofHours(8);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public AuthSession create(String name) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthSession session = new AuthSession(token, name, Instant.now().plus(LIFETIME));
        sessions.put(token, session);
        return session;
    }

    public Optional<AuthSession> find(String token) {
        AuthSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void remove(String token) {
        sessions.remove(token);
    }
}

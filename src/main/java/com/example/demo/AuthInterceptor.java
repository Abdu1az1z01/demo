package com.example.demo;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Проверяет каждый запрос к /api: пустит только вошедшего сотрудника муниципальной инспекции.
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_ATTRIBUTE = "authSession";

    private final SessionStore sessions;

    public AuthInterceptor(SessionStore sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Предварительные CORS-запросы браузера и сам вход пропускаем без токена
        if ("OPTIONS".equals(request.getMethod()) || request.getRequestURI().equals("/api/auth/employee")) {
            return true;
        }

        AuthSession session = readToken(request)
                .flatMap(sessions::find)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход"));
        request.setAttribute(SESSION_ATTRIBUTE, session);
        return true;
    }

    private static Optional<String> readToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring("Bearer ".length()).trim());
        }
        return Optional.empty();
    }
}

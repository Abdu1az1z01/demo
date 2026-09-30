package com.example.demo;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Проверяет каждый запрос к /api:
//   без входа — 401;
//   управление сотрудниками и изменение тарифов — только директор (иначе 403);
//   остальное (абоненты, начисления, просмотр тарифов) — любой вошедший сотрудник.
// Заодно отмечает «последнее действие» сотрудника для учёта времени работы.
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_ATTRIBUTE = "authSession";

    private final SessionStore sessions;
    private final WorkTimeService workTime;

    public AuthInterceptor(SessionStore sessions, WorkTimeService workTime) {
        this.sessions = sessions;
        this.workTime = workTime;
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
        workTime.touch(session.workSessionId());

        if (isDirectorOnly(request.getMethod(), request.getRequestURI()) && !session.isDirector()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступно только директору");
        }
        return true;
    }

    private static boolean isDirectorOnly(String method, String path) {
        return path.startsWith("/api/employees")
                || (path.startsWith("/api/tariffs") && !"GET".equals(method));
    }

    private static Optional<String> readToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring("Bearer ".length()).trim());
        }
        return Optional.empty();
    }
}

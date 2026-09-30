package com.example.demo;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Проверяет каждый запрос к /api: есть ли токен входа и можно ли этому пользователю сюда.
//   Сотрудник (EMPLOYEE) — можно всё.
//   Гражданин (CITIZEN) — только просмотр своего абонента и его истории и оплата своих начислений через банк.
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_ATTRIBUTE = "authSession";

    private static final Pattern OWN_SUBSCRIBER = Pattern.compile("^/api/subscribers/(\\d+)(/bills)?$");
    private static final Pattern PAY_BILL = Pattern.compile("^/api/bills/(\\d+)/pay$");

    private final SessionStore sessions;
    private final BillRepository bills;

    public AuthInterceptor(SessionStore sessions, BillRepository bills) {
        this.sessions = sessions;
        this.bills = bills;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // Предварительные CORS-запросы браузера и сам вход пропускаем без токена
        if ("OPTIONS".equals(request.getMethod())
                || path.equals("/api/auth/employee") || path.equals("/api/auth/citizen")) {
            return true;
        }

        AuthSession session = readToken(request)
                .flatMap(sessions::find)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход"));
        request.setAttribute(SESSION_ATTRIBUTE, session);

        if (session.isEmployee() || path.startsWith("/api/auth/") || citizenAllowed(session, request.getMethod(), path)) {
            return true;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа");
    }

    private boolean citizenAllowed(AuthSession session, String method, String path) {
        Matcher own = OWN_SUBSCRIBER.matcher(path);
        if (own.matches() && Long.valueOf(own.group(1)).equals(session.subscriberId())) {
            return "GET".equals(method);
        }
        Matcher pay = PAY_BILL.matcher(path);
        if (pay.matches() && "POST".equals(method)) {
            return bills.findById(Long.valueOf(pay.group(1)))
                    .map(bill -> bill.getSubscriber().getId().equals(session.subscriberId()))
                    .orElse(false);
        }
        return false;
    }

    private static Optional<String> readToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring("Bearer ".length()).trim());
        }
        return Optional.empty();
    }
}

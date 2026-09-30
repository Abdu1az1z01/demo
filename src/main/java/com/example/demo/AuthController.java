package com.example.demo;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Вход и выход сотрудников муниципальной инспекции (по логину и паролю)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record EmployeeLogin(String login, String password) {
    }

    private final EmployeeRepository employees;
    private final SessionStore sessions;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WorkTimeService workTime;

    public AuthController(EmployeeRepository employees, SessionStore sessions,
                          BCryptPasswordEncoder passwordEncoder, WorkTimeService workTime) {
        this.employees = employees;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.workTime = workTime;
    }

    // POST /api/auth/employee  { "login": "inspector", "password": "..." }
    @PostMapping("/employee")
    public AuthSession loginEmployee(@RequestBody EmployeeLogin request) {
        String login = request.login() == null ? "" : request.login().trim();
        String password = request.password() == null ? "" : request.password();
        Employee employee = employees.findByLogin(login)
                .filter(e -> passwordEncoder.matches(password, e.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));
        if (!employee.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ закрыт. Обратитесь к директору.");
        }
        // Начинаем учёт рабочего времени
        WorkSession workSession = workTime.start(employee);
        return sessions.create(employee, workSession.getId());
    }

    // Кто сейчас вошёл: GET /api/auth/me
    @GetMapping("/me")
    public AuthSession me(@RequestAttribute(AuthInterceptor.SESSION_ATTRIBUTE) AuthSession session) {
        return session;
    }

    // Выйти: POST /api/auth/logout
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestAttribute(AuthInterceptor.SESSION_ATTRIBUTE) AuthSession session) {
        sessions.remove(session.token());
        workTime.end(session.workSessionId());
    }
}

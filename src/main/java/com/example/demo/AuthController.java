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

// Вход и выход: сотрудник — по логину и паролю, гражданин — по лицевому счёту и телефону
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record EmployeeLogin(String login, String password) {
    }

    public record CitizenLogin(String serviceType, String accountNumber, String phone) {
    }

    private final EmployeeRepository employees;
    private final SubscriberRepository subscribers;
    private final SessionStore sessions;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(EmployeeRepository employees, SubscriberRepository subscribers,
                          SessionStore sessions, BCryptPasswordEncoder passwordEncoder) {
        this.employees = employees;
        this.subscribers = subscribers;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    // POST /api/auth/employee  { "login": "inspector", "password": "..." }
    @PostMapping("/employee")
    public AuthSession loginEmployee(@RequestBody EmployeeLogin request) {
        String login = request.login() == null ? "" : request.login().trim();
        String password = request.password() == null ? "" : request.password();
        Employee employee = employees.findByLogin(login)
                .filter(e -> passwordEncoder.matches(password, e.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));
        return sessions.create(AuthSession.Role.EMPLOYEE, employee.getFullName(), null, null);
    }

    // POST /api/auth/citizen  { "serviceType": "gas", "accountNumber": "502030", "phone": "+996 550 120000" }
    // Телефон сравнивается только по цифрам, поэтому пробелы и «+» не важны.
    @PostMapping("/citizen")
    public AuthSession loginCitizen(@RequestBody CitizenLogin request) {
        String account = request.accountNumber() == null ? "" : request.accountNumber().trim();
        String phoneDigits = digits(request.phone());
        Subscriber subscriber = subscribers.findByAccountNumberAndServiceType(account, request.serviceType())
                .filter(s -> !phoneDigits.isEmpty() && phoneDigits.equals(digits(s.getPhone())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Абонент с таким лицевым счётом и телефоном не найден"));
        return sessions.create(AuthSession.Role.CITIZEN, subscriber.getOwnerName(),
                subscriber.getId(), subscriber.getServiceType());
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
    }

    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}

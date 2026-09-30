package com.example.demo;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Управление сотрудниками — только для директора (проверяет AuthInterceptor):
// список, добавление, роль, место работы, доступ, смена пароля и время работы.
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    // Сотрудник для таблицы директора
    public record EmployeeView(Long id, String login, String fullName, Employee.Role role, String workplace,
                               boolean active, boolean online, Instant lastLoginAt, Instant lastSeenAt,
                               long minutesThisMonth) {
    }

    public record EmployeeRequest(String login, String password, String fullName, Employee.Role role,
                                  String workplace, Boolean active) {
    }

    public record SessionView(Instant startedAt, Instant finishedAt, boolean ended, boolean online, long minutes) {
    }

    public record WorkTimeView(String month, long totalMinutes, List<SessionView> sessions) {
    }

    private final EmployeeRepository employees;
    private final WorkSessionRepository workSessions;
    private final SessionStore sessions;
    private final BCryptPasswordEncoder passwordEncoder;

    public EmployeeController(EmployeeRepository employees, WorkSessionRepository workSessions,
                              SessionStore sessions, BCryptPasswordEncoder passwordEncoder) {
        this.employees = employees;
        this.workSessions = workSessions;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    // Все сотрудники: GET /api/employees
    @GetMapping
    @Transactional(readOnly = true)
    public List<EmployeeView> list() {
        return employees.findAll().stream()
                .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                .map(this::toView)
                .toList();
    }

    // Добавить сотрудника: POST /api/employees
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EmployeeView create(@RequestBody EmployeeRequest request) {
        String login = trim(request.login());
        String fullName = trim(request.fullName());
        if (login.isEmpty() || fullName.isEmpty() || request.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заполните логин, ФИО и роль");
        }
        checkPassword(request.password());
        if (employees.findByLogin(login).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Такой логин уже занят");
        }
        Employee employee = employees.save(new Employee(login, passwordEncoder.encode(request.password()), fullName,
                "Муниципальная инспекция г. Бишкек", request.role(), trim(request.workplace())));
        return toView(employee);
    }

    // Изменить сотрудника (ФИО, роль, место работы, доступ, новый пароль): PUT /api/employees/1
    @PutMapping("/{id}")
    @Transactional
    public EmployeeView update(@PathVariable Long id, @RequestBody EmployeeRequest request,
                               @RequestAttribute(AuthInterceptor.SESSION_ATTRIBUTE) AuthSession session) {
        Employee employee = find(id);
        String fullName = trim(request.fullName());
        if (fullName.isEmpty() || request.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заполните ФИО и роль");
        }
        boolean active = request.active() == null || request.active();
        boolean isSelf = id.equals(session.employeeId());
        if (isSelf && (!active || request.role() != Employee.Role.DIRECTOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя закрыть доступ или снять роль директора у самого себя");
        }
        boolean accessChanged = employee.getRole() != request.role() || employee.isActive() != active;
        employee.update(fullName, request.role(), trim(request.workplace()), active);
        if (request.password() != null && !request.password().isEmpty()) {
            checkPassword(request.password());
            employee.setPasswordHash(passwordEncoder.encode(request.password()));
            accessChanged = true;
        }
        // Роль, доступ или пароль изменились — сотруднику нужно войти заново
        if (accessChanged && !isSelf) {
            sessions.removeByEmployee(id);
        }
        return toView(employees.save(employee));
    }

    // Время работы за месяц: GET /api/employees/1/work-time?month=2026-09
    @GetMapping("/{id}/work-time")
    @Transactional(readOnly = true)
    public WorkTimeView workTime(@PathVariable Long id, @RequestParam(required = false) String month) {
        find(id);
        YearMonth ym = month == null ? YearMonth.now(TariffService.ZONE) : YearMonth.parse(month);
        Instant from = ym.atDay(1).atStartOfDay(TariffService.ZONE).toInstant();
        Instant to = ym.plusMonths(1).atDay(1).atStartOfDay(TariffService.ZONE).toInstant();
        Instant now = Instant.now();
        List<SessionView> list = workSessions.findByEmployeeIdAndStartedAtGreaterThanEqualOrderByStartedAtDesc(id, from)
                .stream()
                .filter(ws -> ws.getStartedAt().isBefore(to))
                .map(ws -> new SessionView(ws.getStartedAt(), ws.getFinishedAt(), ws.getEndedAt() != null,
                        ws.isOnline(now), ws.getMinutes()))
                .toList();
        long total = list.stream().mapToLong(SessionView::minutes).sum();
        return new WorkTimeView(ym.toString(), total, list);
    }

    private EmployeeView toView(Employee e) {
        Instant now = Instant.now();
        Instant monthStart = YearMonth.now(TariffService.ZONE).atDay(1).atStartOfDay(TariffService.ZONE).toInstant();
        List<WorkSession> month = workSessions.findByEmployeeIdAndStartedAtGreaterThanEqualOrderByStartedAtDesc(e.getId(), monthStart);
        WorkSession last = workSessions.findFirstByEmployeeIdOrderByStartedAtDesc(e.getId()).orElse(null);
        return new EmployeeView(e.getId(), e.getLogin(), e.getFullName(), e.getRole(), e.getWorkplace(), e.isActive(),
                last != null && last.isOnline(now),
                last == null ? null : last.getStartedAt(),
                last == null ? null : last.getFinishedAt(),
                month.stream().mapToLong(WorkSession::getMinutes).sum());
    }

    private Employee find(Long id) {
        return employees.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сотрудник не найден"));
    }

    private static void checkPassword(String password) {
        if (password == null || password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль — не короче 6 символов");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

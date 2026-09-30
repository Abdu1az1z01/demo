package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

// Создаёт первого сотрудника, если в таблице EMPLOYEES никого нет.
// Логин и пароль берутся из application.properties (zetta.admin.*).
@Component
public class EmployeeSeeder implements CommandLineRunner {

    private final EmployeeRepository employees;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String login;
    private final String password;

    public EmployeeSeeder(EmployeeRepository employees, BCryptPasswordEncoder passwordEncoder,
                          @Value("${zetta.admin.login}") String login,
                          @Value("${zetta.admin.password}") String password) {
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
        this.login = login;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (employees.count() > 0) {
            return;
        }
        employees.save(new Employee(login, passwordEncoder.encode(password),
                "Инспектор", "Муниципальная инспекция г. Бишкек"));
    }
}

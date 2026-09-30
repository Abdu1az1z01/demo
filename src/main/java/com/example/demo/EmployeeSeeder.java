package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Готовит сотрудников при запуске:
//   сотрудникам из старой версии (без роли) ставит роль «инспектор»;
//   если директора нет — создаёт его (zetta.director.* в application.properties);
//   если база совсем пустая — создаёт ещё и тестового инспектора (zetta.inspector.*).
@Component
public class EmployeeSeeder implements CommandLineRunner {

    private static final String ORGANIZATION = "Муниципальная инспекция г. Бишкек";

    private final EmployeeRepository employees;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String directorLogin;
    private final String directorPassword;
    private final String inspectorLogin;
    private final String inspectorPassword;

    public EmployeeSeeder(EmployeeRepository employees, BCryptPasswordEncoder passwordEncoder,
                          @Value("${zetta.director.login}") String directorLogin,
                          @Value("${zetta.director.password}") String directorPassword,
                          @Value("${zetta.inspector.login}") String inspectorLogin,
                          @Value("${zetta.inspector.password}") String inspectorPassword) {
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
        this.directorLogin = directorLogin;
        this.directorPassword = directorPassword;
        this.inspectorLogin = inspectorLogin;
        this.inspectorPassword = inspectorPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean emptyDatabase = employees.count() == 0;

        for (Employee employee : employees.findAll()) {
            if (employee.getRole() == null) {
                employee.setRole(Employee.Role.INSPECTOR);
            }
        }

        boolean hasDirector = employees.findAll().stream().anyMatch(e -> e.getRole() == Employee.Role.DIRECTOR);
        if (!hasDirector && employees.findByLogin(directorLogin).isEmpty()) {
            employees.save(new Employee(directorLogin, passwordEncoder.encode(directorPassword),
                    "Директор", ORGANIZATION, Employee.Role.DIRECTOR, "Главный офис"));
        }

        if (emptyDatabase) {
            employees.save(new Employee(inspectorLogin, passwordEncoder.encode(inspectorPassword),
                    "Инспектор", ORGANIZATION, Employee.Role.INSPECTOR, "Октябрьский район"));
        }
    }
}

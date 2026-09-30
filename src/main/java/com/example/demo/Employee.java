package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Сотрудник муниципального предприятия / инспекции. Таблица EMPLOYEES.
// Пароль хранится только в виде хеша BCrypt, сам пароль в базе не лежит.
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String login;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Например «Муниципальная инспекция г. Бишкек»
    private String organization;

    protected Employee() {
    }

    public Employee(String login, String passwordHash, String fullName, String organization) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.organization = organization;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getOrganization() { return organization; }
}

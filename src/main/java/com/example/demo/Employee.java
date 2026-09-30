package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Сотрудник муниципальной инспекции. Таблица EMPLOYEES.
// Пароль хранится только в виде хеша BCrypt, сам пароль в базе не лежит.
@Entity
@Table(name = "employees")
public class Employee {

    // DIRECTOR — директор (управляет сотрудниками и тарифами), INSPECTOR — инспектор (работает с абонентами)
    public enum Role { DIRECTOR, INSPECTOR }

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

    @Enumerated(EnumType.STRING)
    private Role role;

    // Место работы / участок, которое назначил директор (например «Октябрьский район»)
    private String workplace;

    // false — доступ закрыт (сотрудник уволен или временно отстранён), войти нельзя
    private Boolean active;

    protected Employee() {
    }

    public Employee(String login, String passwordHash, String fullName, String organization,
                    Role role, String workplace) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.organization = organization;
        this.role = role;
        this.workplace = workplace;
        this.active = true;
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getOrganization() { return organization; }
    public Role getRole() { return role; }
    public String getWorkplace() { return workplace; }

    // В базе от старой версии колонки active не было — такие сотрудники считаются активными
    public boolean isActive() { return active == null || active; }

    public void setRole(Role role) { this.role = role; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void update(String fullName, Role role, String workplace, boolean active) {
        this.fullName = fullName;
        this.role = role;
        this.workplace = workplace;
        this.active = active;
    }
}

package com.example.demo;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Рабочая сессия сотрудника: от входа в систему до выхода. Таблица WORK_SESSIONS.
// По ним директор видит время работы.
@Entity
@Table(name = "work_sessions")
public class WorkSession {

    // Если сотрудник не нажал «Выйти», сессия считается законченной после такого времени бездействия
    public static final Duration IDLE_LIMIT = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    // Вход
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    // Последнее действие в системе
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    // Выход (кнопка «Выйти»); null — не выходил
    @Column(name = "ended_at")
    private Instant endedAt;

    protected WorkSession() {
    }

    public WorkSession(Employee employee, Instant startedAt) {
        this.employee = employee;
        this.startedAt = startedAt;
        this.lastSeenAt = startedAt;
    }

    public void touch(Instant now) { this.lastSeenAt = now; }
    public void end(Instant now) { this.endedAt = now; this.lastSeenAt = now; }

    // Сейчас в системе: не выходил и был активен последние 15 минут
    public boolean isOnline(Instant now) {
        return endedAt == null && lastSeenAt.plus(IDLE_LIMIT).isAfter(now);
    }

    // Конец сессии: выход, иначе последнее действие
    public Instant getFinishedAt() {
        return endedAt != null ? endedAt : lastSeenAt;
    }

    public long getMinutes() {
        return Duration.between(startedAt, getFinishedAt()).toMinutes();
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getEndedAt() { return endedAt; }
}

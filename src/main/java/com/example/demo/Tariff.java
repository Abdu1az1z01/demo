package com.example.demo;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Единый тариф услуги (одинаковый для всех абонентов). Таблица TARIFFS.
// Тариф действует с даты effectiveFrom до даты начала следующего тарифа этой же услуги.
// Устанавливает только директор.
@Entity
@Table(name = "tariffs")
public class Tariff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Код услуги: cold-water, hot-water, heating, garbage, gas
    @Column(name = "service_type", nullable = false)
    private String serviceType;

    // Цена в сомах за единицу (м³, Гкал, человек)
    @Column(nullable = false)
    private double price;

    // С какой даты действует
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    // Кто установил и когда
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Tariff() {
    }

    public Tariff(String serviceType, double price, LocalDate effectiveFrom, String createdBy) {
        this.serviceType = serviceType;
        this.price = price;
        this.effectiveFrom = effectiveFrom;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getServiceType() { return serviceType; }
    public double getPrice() { return price; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}

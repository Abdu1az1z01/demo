package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Абонент коммунального предприятия. Каждая запись — строка в таблице SUBSCRIBERS.
@Entity
@Table(name = "subscribers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_number", "service_type"}))
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Номер лицевого счёта
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    // Код услуги: cold-water, hot-water, heating, garbage, gas (совпадает с id в меню фронтенда)
    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    private String address;

    @Column(name = "previous_reading")
    private double previousReading;

    @Column(name = "current_reading")
    private double currentReading;

    // Тариф в сомах за единицу (м³, Гкал, человек)
    private double tariff;

    // Сумма к оплате / долг в сомах
    private double debt;

    protected Subscriber() {
    }

    public Subscriber(String accountNumber, String serviceType, String ownerName, String address,
                      double previousReading, double tariff, double debt) {
        this.accountNumber = accountNumber;
        this.serviceType = serviceType;
        this.ownerName = ownerName;
        this.address = address;
        this.previousReading = previousReading;
        this.currentReading = previousReading;
        this.tariff = tariff;
        this.debt = debt;
    }

    // Принять новые показания: долг растёт на (новые − предыдущие) × тариф
    public void applyReading(double reading) {
        double consumption = reading - previousReading;
        this.debt = Math.round((debt + consumption * tariff) * 100) / 100.0;
        this.currentReading = reading;
        this.previousReading = reading;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getServiceType() { return serviceType; }
    public String getOwnerName() { return ownerName; }
    public String getAddress() { return address; }
    public double getPreviousReading() { return previousReading; }
    public double getCurrentReading() { return currentReading; }
    public double getTariff() { return tariff; }
    public double getDebt() { return debt; }
}

package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    private String phone;

    // Предпоследние и последние переданные показания счётчика
    @Column(name = "previous_reading")
    private double previousReading;

    @Column(name = "current_reading")
    private double currentReading;

    // Устаревшее поле: раньше тариф был свой у каждого абонента.
    // Теперь тариф единый для услуги (таблица TARIFFS), колонка оставлена для совместимости со старой базой.
    @JsonIgnore
    private double tariff;

    // Общий долг = сумма всех неоплаченных начислений (таблица BILLS)
    private double debt;

    protected Subscriber() {
    }

    public Subscriber(String accountNumber, String serviceType, String ownerName, String address,
                      String phone, double previousReading) {
        this.accountNumber = accountNumber;
        this.serviceType = serviceType;
        this.ownerName = ownerName;
        this.address = address;
        this.phone = phone;
        this.previousReading = previousReading;
        this.currentReading = previousReading;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getServiceType() { return serviceType; }
    public String getOwnerName() { return ownerName; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public double getPreviousReading() { return previousReading; }
    public double getCurrentReading() { return currentReading; }
    public double getDebt() { return debt; }

    // Новые показания: последние становятся предпоследними
    public void applyReading(double reading) {
        this.previousReading = this.currentReading;
        this.currentReading = reading;
    }

    public void setDebt(double debt) { this.debt = debt; }

    // Изменить данные абонента (форма «Редактировать» на сайте)
    public void update(String accountNumber, String ownerName, String address, String phone) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.address = address;
        this.phone = phone;
    }
}

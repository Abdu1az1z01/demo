package com.example.demo;

import java.time.LocalDate;

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

// Начисление (квитанция) абонента за один месяц. Каждая запись — строка в таблице BILLS.
@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id")
    private Subscriber subscriber;

    // Расчётный месяц в формате 2026-09
    @Column(nullable = false)
    private String period;

    @Column(name = "previous_reading")
    private double previousReading;

    @Column(name = "current_reading")
    private double currentReading;

    // Расход = текущие − предыдущие показания
    private double consumption;

    // Тариф, по которому посчитано начисление (действовал на дату начисления)
    private Double tariff;

    // Сумма к оплате = расход × тариф
    private double amount;

    // Оплачено или нет (на фронтенде: зелёный / красный)
    private boolean paid;

    @Column(name = "paid_at")
    private LocalDate paidAt;

    // Через какой банк оплачено (например «MBank»)
    @Column(name = "paid_via")
    private String paidVia;

    // Причина, если статус оплаты вручную изменила инспекция (ошибка при оплате и т.п.)
    @Column(name = "status_note")
    private String statusNote;

    protected Bill() {
    }

    public Bill(Subscriber subscriber, String period, double previousReading, double currentReading, double tariff) {
        this.subscriber = subscriber;
        this.period = period;
        this.previousReading = previousReading;
        this.currentReading = currentReading;
        this.consumption = Math.round((currentReading - previousReading) * 100) / 100.0;
        this.tariff = tariff;
        this.amount = Math.round(consumption * tariff * 100) / 100.0;
    }

    // Для начислений из старой версии: восстановить тариф как сумма / расход
    public void restoreTariff() {
        if (tariff == null && consumption > 0) {
            tariff = Math.round(amount / consumption * 100) / 100.0;
        }
    }

    // Оплачено через банк (в тестовых данных)
    public void markPaid(LocalDate date, String via) {
        this.paid = true;
        this.paidAt = date;
        this.paidVia = via;
    }

    // Инспекция вручную меняет статус (например, банк списал деньги, но оплата не отметилась)
    public void changeStatus(boolean paid, String note, LocalDate date) {
        this.paid = paid;
        this.paidAt = paid ? (this.paidAt != null ? this.paidAt : date) : null;
        if (!paid) {
            this.paidVia = null;
        }
        this.statusNote = note;
    }

    public Long getId() { return id; }
    public Subscriber getSubscriber() { return subscriber; }
    public String getPeriod() { return period; }
    public double getPreviousReading() { return previousReading; }
    public double getCurrentReading() { return currentReading; }
    public double getConsumption() { return consumption; }
    public Double getTariff() { return tariff; }
    public double getAmount() { return amount; }
    public boolean isPaid() { return paid; }
    public LocalDate getPaidAt() { return paidAt; }
    public String getPaidVia() { return paidVia; }
    public String getStatusNote() { return statusNote; }
}

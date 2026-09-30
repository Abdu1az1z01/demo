package com.example.demo;

// Данные из формы «Добавить / Редактировать абонента», которые присылает фронтенд
public record SubscriberRequest(
        String accountNumber,
        String ownerName,
        String address,
        String phone,
        Double tariff,
        Double currentReading) {

    public String accountNumberTrimmed() { return trim(accountNumber); }
    public String ownerNameTrimmed() { return trim(ownerName); }
    public String addressTrimmed() { return trim(address); }
    public String phoneTrimmed() { return trim(phone); }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

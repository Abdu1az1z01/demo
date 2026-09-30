package com.example.demo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// Какой тариф действует для услуги на нужную дату
@Service
public class TariffService {

    // Часовой пояс Бишкека — чтобы «сегодня» было бишкекским
    public static final ZoneId ZONE = ZoneId.of("Asia/Bishkek");

    // Коды услуг (совпадают с id в меню фронтенда)
    public static final List<String> SERVICE_TYPES = List.of("cold-water", "hot-water", "heating", "garbage", "gas");

    private final TariffRepository tariffs;

    public TariffService(TariffRepository tariffs) {
        this.tariffs = tariffs;
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public double priceOn(String serviceType, LocalDate date) {
        return tariffs.findFirstByServiceTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(serviceType, date)
                .map(Tariff::getPrice)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Для услуги не установлен тариф. Обратитесь к директору."));
    }

    public double currentPrice(String serviceType) {
        return priceOn(serviceType, today());
    }
}

package com.example.demo;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Заполняет базу тестовыми абонентами при первом запуске (если таблица пустая).
@Component
public class DataSeeder implements CommandLineRunner {

    private record Service(String code, String accountPrefix, double tariff) {
    }

    // Тарифы примерные, для демонстрации
    private static final List<Service> SERVICES = List.of(
            new Service("cold-water", "10", 10.45),
            new Service("hot-water", "20", 95.60),
            new Service("heating", "30", 1520.00),
            new Service("garbage", "40", 45.00),
            new Service("gas", "50", 13.50));

    private static final List<String> NAMES = List.of(
            "Асанов Бакыт Эркинович",
            "Иванова Мария Петровна",
            "Токтогулов Нурлан Алмазбекович",
            "Сыдыкова Айгуль Маратовна",
            "Ким Сергей Владимирович",
            "Жумабаева Чолпон Асановна",
            "Осмонов Эрлан Кубанычбекович",
            "Петров Андрей Николаевич",
            "Абдыкадырова Гульнара Тологоновна",
            "Мамытов Азамат Русланович");

    private static final List<String> ADDRESSES = List.of(
            "г. Бишкек, ул. Киевская 112, кв. 5",
            "г. Бишкек, пр. Чуй 45, кв. 17",
            "г. Бишкек, мкр. Джал 23, д. 8, кв. 41",
            "г. Бишкек, ул. Токтогула 98, кв. 3",
            "г. Бишкек, мкр. Восток-5, д. 12, кв. 60",
            "г. Бишкек, ул. Ахунбаева 150, кв. 22",
            "г. Бишкек, пр. Манаса 67, кв. 9",
            "г. Бишкек, мкр. Асанбай 14, кв. 33",
            "г. Бишкек, ул. Боконбаева 201, кв. 1",
            "г. Бишкек, мкр. Тунгуч 30, кв. 75");

    private final SubscriberRepository repository;

    public DataSeeder(SubscriberRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        for (Service service : SERVICES) {
            for (int i = 0; i < NAMES.size(); i++) {
                String account = service.accountPrefix() + String.format("%04d", 2030 + i);
                double previousReading = 100 + i * 17;
                double debt = (i % 3 == 0) ? 0 : Math.round(service.tariff() * (5 + i * 3) * 100) / 100.0;
                repository.save(new Subscriber(account, service.code(), NAMES.get(i), ADDRESSES.get(i),
                        previousReading, service.tariff(), debt));
            }
        }
    }
}

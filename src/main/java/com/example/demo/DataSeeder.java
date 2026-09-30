package com.example.demo;

import java.time.YearMonth;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Заполняет базу тестовыми абонентами и историей начислений за 6 месяцев.
// Срабатывает, если таблица начислений пустая (первый запуск или база от старой версии).
@Component
public class DataSeeder implements CommandLineRunner {

    private record Service(String code, String accountPrefix, double tariff, double monthlyUsage) {
    }

    // Тарифы и средний расход в месяц — примерные, для демонстрации
    private static final List<Service> SERVICES = List.of(
            new Service("cold-water", "10", 10.45, 8),
            new Service("hot-water", "20", 95.60, 4),
            new Service("heating", "30", 1520.00, 1.2),
            new Service("garbage", "40", 45.00, 3),
            new Service("gas", "50", 13.50, 25));

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
            "Мамытов Азамат Русланович",
            "Шарипова Динара Кадыровна",
            "Бектурганов Тимур Садырович",
            "Лебедева Ольга Игоревна",
            "Усенов Айбек Жыргалбекович",
            "Эсенбаева Нуриза Талантовна",
            "Ли Виктор Александрович",
            "Кадыров Мирлан Бакирович",
            "Сагынбаева Бермет Айтбековна",
            "Николаев Дмитрий Сергеевич",
            "Турдубаев Эмиль Нурланович");

    private static final List<String> STREETS = List.of(
            "ул. Киевская", "пр. Чуй", "мкр. Джал", "ул. Токтогула", "мкр. Восток-5",
            "ул. Ахунбаева", "пр. Манаса", "мкр. Асанбай", "ул. Боконбаева", "мкр. Тунгуч");

    private static final int MONTHS_OF_HISTORY = 6;

    // Банки, через которые «оплачены» тестовые начисления
    private static final List<String> BANKS = List.of("MBank", "Optima Bank", "Bakai Bank", "О!Деньги", "Элсом");

    private final SubscriberRepository subscribers;
    private final BillRepository bills;

    public DataSeeder(SubscriberRepository subscribers, BillRepository bills) {
        this.subscribers = subscribers;
        this.bills = bills;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bills.count() > 0) {
            return;
        }
        subscribers.deleteAllInBatch();

        YearMonth now = YearMonth.now();
        for (Service service : SERVICES) {
            for (int i = 0; i < NAMES.size(); i++) {
                String account = service.accountPrefix() + String.format("%04d", 2030 + i);
                String address = "г. Бишкек, " + STREETS.get(i % STREETS.size()) + " " + (10 + i * 7) + ", кв. " + (i * 3 + 1);
                String phone = "+996 " + (550 + i) + " " + String.format("%06d", 120000 + i * 3571);
                Subscriber subscriber = subscribers.save(
                        new Subscriber(account, service.code(), NAMES.get(i), address, phone, 100 + i * 17, service.tariff()));

                // История за последние месяцы; у части абонентов последние 1–2 месяца не оплачены
                int unpaidMonths = i % 3;
                double reading = subscriber.getCurrentReading();
                for (int m = MONTHS_OF_HISTORY; m >= 1; m--) {
                    YearMonth period = now.minusMonths(m);
                    double usage = Math.round(service.monthlyUsage() * (0.8 + ((i + m) % 5) * 0.1) * 10) / 10.0;
                    double next = Math.round((reading + usage) * 10) / 10.0;
                    Bill bill = new Bill(subscriber, period.toString(), reading, next);
                    if (m > unpaidMonths) {
                        bill.markPaid(period.plusMonths(1).atDay(10), BANKS.get((i + m) % BANKS.size()));
                    }
                    bills.save(bill);
                    subscriber.applyReading(next);
                    reading = next;
                }
                bills.flush();
                subscriber.setDebt(bills.sumUnpaid(subscriber.getId()));
                subscribers.save(subscriber);
            }
        }
    }
}

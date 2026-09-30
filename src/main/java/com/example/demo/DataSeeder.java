package com.example.demo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Заполняет базу тестовыми данными (только если zetta.demo-data=true):
//   тарифы — если таблица TARIFFS пустая (старый тариф с 2020 года и повышение 3 месяца назад);
//   тариф в начислениях из старой версии — восстанавливается как сумма / расход;
//   абоненты и история начислений за 6 месяцев — если таблица начислений пустая.
@Component
public class DataSeeder implements CommandLineRunner {

    private record Service(String code, String accountPrefix, double tariff, double monthlyUsage) {
    }

    // Тарифы (текущие) и средний расход в месяц — примерные, для демонстрации
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

    // Старый тариф до повышения = 90% от текущего
    private static final double OLD_TARIFF_RATIO = 0.9;

    private final SubscriberRepository subscribers;
    private final BillRepository bills;
    private final TariffRepository tariffRepository;
    private final TariffService tariffs;
    private final boolean enabled;

    public DataSeeder(SubscriberRepository subscribers, BillRepository bills,
                      TariffRepository tariffRepository, TariffService tariffs,
                      @Value("${zetta.demo-data:true}") boolean enabled) {
        this.subscribers = subscribers;
        this.bills = bills;
        this.tariffRepository = tariffRepository;
        this.tariffs = tariffs;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            // Без тестовых данных всё равно восстанавливаем тариф в старых начислениях
            bills.findByTariffIsNull().forEach(Bill::restoreTariff);
            return;
        }
        YearMonth now = YearMonth.now(TariffService.ZONE);
        seedTariffs(now);

        if (bills.count() > 0) {
            // База от старой версии: восстанавливаем тариф в начислениях, где его нет
            bills.findByTariffIsNull().forEach(Bill::restoreTariff);
            return;
        }
        subscribers.deleteAllInBatch();

        for (Service service : SERVICES) {
            for (int i = 0; i < NAMES.size(); i++) {
                String account = service.accountPrefix() + String.format("%04d", 2030 + i);
                String address = "г. Бишкек, " + STREETS.get(i % STREETS.size()) + " " + (10 + i * 7) + ", кв. " + (i * 3 + 1);
                String phone = "+996 " + (550 + i) + " " + String.format("%06d", 120000 + i * 3571);
                Subscriber subscriber = subscribers.save(
                        new Subscriber(account, service.code(), NAMES.get(i), address, phone, 100 + i * 17));

                // История за последние месяцы; у части абонентов последние 1–2 месяца не оплачены
                int unpaidMonths = i % 3;
                double reading = subscriber.getCurrentReading();
                for (int m = MONTHS_OF_HISTORY; m >= 1; m--) {
                    YearMonth period = now.minusMonths(m);
                    double usage = Math.round(service.monthlyUsage() * (0.8 + ((i + m) % 5) * 0.1) * 10) / 10.0;
                    double next = Math.round((reading + usage) * 10) / 10.0;
                    // Показания передаются в конце месяца — по тарифу, действовавшему на эту дату
                    double price = tariffs.priceOn(service.code(), period.atEndOfMonth());
                    Bill bill = new Bill(subscriber, period.toString(), reading, next, price);
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

    private void seedTariffs(YearMonth now) {
        if (tariffRepository.count() > 0) {
            return;
        }
        LocalDate raisedFrom = now.minusMonths(3).atDay(1);
        for (Service service : SERVICES) {
            double oldPrice = Math.round(service.tariff() * OLD_TARIFF_RATIO * 100) / 100.0;
            tariffRepository.save(new Tariff(service.code(), oldPrice, LocalDate.of(2020, 1, 1), "Начальные данные"));
            tariffRepository.save(new Tariff(service.code(), service.tariff(), raisedFrom, "Начальные данные"));
        }
        tariffRepository.flush();
    }
}

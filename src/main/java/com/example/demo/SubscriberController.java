package com.example.demo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// REST API для фронтенда. CORS разрешён для Angular на любом порту localhost / 127.0.0.1.
@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class SubscriberController {

    public record ReadingRequest(double reading) {
    }

    private final SubscriberRepository subscribers;
    private final BillRepository bills;

    public SubscriberController(SubscriberRepository subscribers, BillRepository bills) {
        this.subscribers = subscribers;
        this.bills = bills;
    }

    // Список абонентов предприятия: GET /api/services/cold-water/subscribers?search=иванов
    @GetMapping("/services/{service}/subscribers")
    public List<Subscriber> list(@PathVariable String service,
                                 @RequestParam(defaultValue = "") String search) {
        return subscribers.search(service, search.trim());
    }

    // Поиск сразу во всех услугах: GET /api/subscribers?search=иванов
    @GetMapping("/subscribers")
    public List<Subscriber> searchAll(@RequestParam(defaultValue = "") String search) {
        return subscribers.search(null, search.trim());
    }

    // Добавить абонента: POST /api/services/cold-water/subscribers  { "ownerName": ..., "accountNumber": ... }
    @PostMapping("/services/{service}/subscribers")
    @ResponseStatus(HttpStatus.CREATED)
    public Subscriber create(@PathVariable String service, @RequestBody SubscriberRequest request) {
        validate(request);
        if (subscribers.existsByAccountNumberAndServiceType(request.accountNumberTrimmed(), service)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Лицевой счёт уже занят в этой услуге");
        }
        double reading = request.currentReading() == null ? 0 : request.currentReading();
        return subscribers.save(new Subscriber(request.accountNumberTrimmed(), service, request.ownerNameTrimmed(),
                request.addressTrimmed(), request.phoneTrimmed(), reading, request.tariff()));
    }

    // Изменить абонента (ФИО, лицевой счёт, адрес, телефон, тариф): PUT /api/subscribers/1
    @PutMapping("/subscribers/{id}")
    @Transactional
    public Subscriber update(@PathVariable Long id, @RequestBody SubscriberRequest request) {
        validate(request);
        Subscriber subscriber = findSubscriber(id);
        if (subscribers.existsByAccountNumberAndServiceTypeAndIdNot(
                request.accountNumberTrimmed(), subscriber.getServiceType(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Лицевой счёт уже занят в этой услуге");
        }
        subscriber.update(request.accountNumberTrimmed(), request.ownerNameTrimmed(),
                request.addressTrimmed(), request.phoneTrimmed(), request.tariff());
        return subscribers.save(subscriber);
    }

    // Удалить абонента вместе с историей начислений: DELETE /api/subscribers/1
    @DeleteMapping("/subscribers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        Subscriber subscriber = findSubscriber(id);
        bills.deleteBySubscriberId(id);
        subscribers.delete(subscriber);
    }

    // Один абонент: GET /api/subscribers/1
    @GetMapping("/subscribers/{id}")
    public Subscriber get(@PathVariable Long id) {
        return findSubscriber(id);
    }

    // История начислений абонента: GET /api/subscribers/1/bills
    @GetMapping("/subscribers/{id}/bills")
    public List<Bill> history(@PathVariable Long id) {
        findSubscriber(id);
        return bills.findBySubscriberIdOrderByPeriodDescIdDesc(id);
    }

    // Передать показания: создаётся новое неоплаченное начисление за текущий месяц
    // POST /api/subscribers/1/readings  { "reading": 130 }
    @PostMapping("/subscribers/{id}/readings")
    @Transactional
    public Subscriber submitReading(@PathVariable Long id, @RequestBody ReadingRequest request) {
        Subscriber subscriber = findSubscriber(id);
        if (request.reading() < subscriber.getCurrentReading()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Новые показания не могут быть меньше предыдущих");
        }
        bills.save(new Bill(subscriber, YearMonth.now().toString(),
                subscriber.getCurrentReading(), request.reading()));
        subscriber.applyReading(request.reading());
        return recalculateDebt(subscriber);
    }

    // Оплатить начисление: POST /api/bills/5/pay
    @PostMapping("/bills/{billId}/pay")
    @Transactional
    public Subscriber pay(@PathVariable Long billId) {
        Bill bill = bills.findById(billId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Начисление не найдено"));
        if (!bill.isPaid()) {
            bill.markPaid(LocalDate.now());
            bills.save(bill);
        }
        return recalculateDebt(bill.getSubscriber());
    }

    private Subscriber recalculateDebt(Subscriber subscriber) {
        bills.flush();
        subscriber.setDebt(bills.sumUnpaid(subscriber.getId()));
        return subscribers.save(subscriber);
    }

    private void validate(SubscriberRequest request) {
        if (request.ownerNameTrimmed().isEmpty() || request.accountNumberTrimmed().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ФИО и лицевой счёт обязательны");
        }
        if (request.tariff() == null || request.tariff() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите тариф (0 или больше)");
        }
    }

    private Subscriber findSubscriber(Long id) {
        return subscribers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Абонент не найден"));
    }
}

package com.example.demo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
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

// REST API для фронтенда. Доступ только для вошедших сотрудников инспекции (проверяет AuthInterceptor).
@RestController
@RequestMapping("/api")
public class SubscriberController {

    public record ReadingRequest(double reading) {
    }

    // Ручное изменение статуса инспекцией: { "paid": true, "note": "Банк подтвердил оплату, чек №123" }
    public record StatusRequest(Boolean paid, String note) {
    }

    private final SubscriberRepository subscribers;
    private final BillRepository bills;
    private final TariffService tariffs;

    public SubscriberController(SubscriberRepository subscribers, BillRepository bills, TariffService tariffs) {
        this.subscribers = subscribers;
        this.bills = bills;
        this.tariffs = tariffs;
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
                request.addressTrimmed(), request.phoneTrimmed(), reading));
    }

    // Изменить абонента (ФИО, лицевой счёт, адрес, телефон): PUT /api/subscribers/1
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
                request.addressTrimmed(), request.phoneTrimmed());
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

    // Передать показания: создаётся новое неоплаченное начисление за текущий месяц по текущему тарифу
    // POST /api/subscribers/1/readings  { "reading": 130 }
    @PostMapping("/subscribers/{id}/readings")
    @Transactional
    public Subscriber submitReading(@PathVariable Long id, @RequestBody ReadingRequest request) {
        Subscriber subscriber = findSubscriber(id);
        if (request.reading() < subscriber.getCurrentReading()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Новые показания не могут быть меньше предыдущих");
        }
        // Начисление считается по единому тарифу, который действует сегодня
        bills.save(new Bill(subscriber, YearMonth.now(TariffService.ZONE).toString(),
                subscriber.getCurrentReading(), request.reading(),
                tariffs.currentPrice(subscriber.getServiceType())));
        subscriber.applyReading(request.reading());
        return recalculateDebt(subscriber);
    }

    // Изменить статус оплаты вручную (например, ошибка при оплате через банк): PUT /api/bills/5/status
    @PutMapping("/bills/{billId}/status")
    @Transactional
    public Subscriber changeStatus(@PathVariable Long billId, @RequestBody StatusRequest request) {
        String note = request.note() == null ? "" : request.note().trim();
        if (request.paid() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите новый статус");
        }
        if (note.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите причину изменения статуса");
        }
        Bill bill = findBill(billId);
        bill.changeStatus(request.paid(), note, TariffService.today());
        bills.save(bill);
        return recalculateDebt(bill.getSubscriber());
    }

    private Bill findBill(Long billId) {
        return bills.findById(billId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Начисление не найдено"));
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
    }

    private Subscriber findSubscriber(Long id) {
        return subscribers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Абонент не найден"));
    }
}

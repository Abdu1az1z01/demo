package com.example.demo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private Subscriber findSubscriber(Long id) {
        return subscribers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Абонент не найден"));
    }
}

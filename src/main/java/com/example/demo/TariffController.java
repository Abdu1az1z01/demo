package com.example.demo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Единые тарифы. Смотреть могут все сотрудники, устанавливать и отменять — только директор
// (проверяет AuthInterceptor).
@RestController
@RequestMapping("/api/tariffs")
public class TariffController {

    // PAST — уже не действует, CURRENT — действует сейчас, PLANNED — начнёт действовать в будущем
    public enum Status { PAST, CURRENT, PLANNED }

    public record TariffView(Long id, String serviceType, double price, LocalDate effectiveFrom,
                             String createdBy, Status status) {
    }

    public record TariffRequest(String serviceType, Double price, LocalDate effectiveFrom) {
    }

    private final TariffRepository tariffs;

    public TariffController(TariffRepository tariffs) {
        this.tariffs = tariffs;
    }

    // Все тарифы с историей: GET /api/tariffs
    @GetMapping
    public List<TariffView> list() {
        LocalDate today = TariffService.today();
        List<Tariff> all = tariffs.findAllByOrderByServiceTypeAscEffectiveFromDescIdDesc();
        return all.stream().map(t -> new TariffView(t.getId(), t.getServiceType(), t.getPrice(),
                t.getEffectiveFrom(), t.getCreatedBy(), statusOf(t, all, today))).toList();
    }

    // Установить новый тариф: POST /api/tariffs  { "serviceType": "gas", "price": 15, "effectiveFrom": "2026-11-01" }
    // Уже созданные начисления не пересчитываются — новый тариф применяется к начислениям с этой даты.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TariffView create(@RequestBody TariffRequest request,
                             @RequestAttribute(AuthInterceptor.SESSION_ATTRIBUTE) AuthSession session) {
        if (!TariffService.SERVICE_TYPES.contains(request.serviceType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестная услуга");
        }
        if (request.price() == null || request.price() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Тариф должен быть больше нуля");
        }
        LocalDate today = TariffService.today();
        if (request.effectiveFrom() == null || request.effectiveFrom().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата начала — сегодня или позже");
        }
        if (tariffs.existsByServiceTypeAndEffectiveFrom(request.serviceType(), request.effectiveFrom())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "С этой даты тариф уже установлен");
        }
        double price = Math.round(request.price() * 100) / 100.0;
        Tariff saved = tariffs.save(new Tariff(request.serviceType(), price, request.effectiveFrom(), session.name()));
        return new TariffView(saved.getId(), saved.getServiceType(), saved.getPrice(), saved.getEffectiveFrom(),
                saved.getCreatedBy(), saved.getEffectiveFrom().isAfter(today) ? Status.PLANNED : Status.CURRENT);
    }

    // Отменить запланированный тариф (который ещё не начал действовать): DELETE /api/tariffs/5
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        Tariff tariff = tariffs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тариф не найден"));
        if (!tariff.getEffectiveFrom().isAfter(TariffService.today())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Можно отменить только тариф, который ещё не начал действовать");
        }
        tariffs.delete(tariff);
    }

    private static Status statusOf(Tariff tariff, List<Tariff> all, LocalDate today) {
        if (tariff.getEffectiveFrom().isAfter(today)) {
            return Status.PLANNED;
        }
        // Действующий — самый поздний из уже начавшихся тарифов этой услуги
        Tariff current = all.stream()
                .filter(t -> t.getServiceType().equals(tariff.getServiceType()) && !t.getEffectiveFrom().isAfter(today))
                .findFirst()   // список отсортирован: сначала новые
                .orElse(null);
        return tariff == current ? Status.CURRENT : Status.PAST;
    }
}

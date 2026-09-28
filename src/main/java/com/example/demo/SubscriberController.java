package com.example.demo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// REST API для фронтенда (Angular работает на http://localhost:4200)
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class SubscriberController {

    public record ReadingRequest(double reading) {
    }

    private final SubscriberRepository repository;

    public SubscriberController(SubscriberRepository repository) {
        this.repository = repository;
    }

    // Список абонентов предприятия: GET /api/services/cold-water/subscribers?search=иванов
    @GetMapping("/services/{service}/subscribers")
    public List<Subscriber> list(@PathVariable String service,
                                 @RequestParam(defaultValue = "") String search) {
        return repository.search(service, search.trim());
    }

    // Квитанция одного абонента: GET /api/subscribers/1
    @GetMapping("/subscribers/{id}")
    public Subscriber get(@PathVariable Long id) {
        return find(id);
    }

    // Передать показания счётчика: POST /api/subscribers/1/readings  { "reading": 130 }
    @PostMapping("/subscribers/{id}/readings")
    public Subscriber submitReading(@PathVariable Long id, @RequestBody ReadingRequest request) {
        Subscriber subscriber = find(id);
        if (request.reading() < subscriber.getPreviousReading()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Новые показания не могут быть меньше предыдущих");
        }
        subscriber.applyReading(request.reading());
        return repository.save(subscriber);
    }

    private Subscriber find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Абонент не найден"));
    }
}

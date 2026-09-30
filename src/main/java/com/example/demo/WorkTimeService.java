package com.example.demo;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Учёт времени работы: вход, последнее действие, выход
@Service
public class WorkTimeService {

    // Как часто обновлять «последнее действие» (чтобы не писать в базу на каждый запрос)
    private static final Duration TOUCH_EVERY = Duration.ofMinutes(1);

    private final WorkSessionRepository workSessions;

    public WorkTimeService(WorkSessionRepository workSessions) {
        this.workSessions = workSessions;
    }

    @Transactional
    public WorkSession start(Employee employee) {
        return workSessions.save(new WorkSession(employee, Instant.now()));
    }

    @Transactional
    public void touch(Long workSessionId) {
        Instant now = Instant.now();
        workSessions.findById(workSessionId)
                .filter(ws -> ws.getEndedAt() == null && ws.getLastSeenAt().plus(TOUCH_EVERY).isBefore(now))
                .ifPresent(ws -> ws.touch(now));
    }

    @Transactional
    public void end(Long workSessionId) {
        workSessions.findById(workSessionId)
                .filter(ws -> ws.getEndedAt() == null)
                .ifPresent(ws -> ws.end(Instant.now()));
    }
}

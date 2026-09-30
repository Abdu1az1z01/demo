package com.example.demo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkSessionRepository extends JpaRepository<WorkSession, Long> {

    // Сессии сотрудника, начатые после даты (сначала новые)
    List<WorkSession> findByEmployeeIdAndStartedAtGreaterThanEqualOrderByStartedAtDesc(Long employeeId, Instant from);

    Optional<WorkSession> findFirstByEmployeeIdOrderByStartedAtDesc(Long employeeId);
}

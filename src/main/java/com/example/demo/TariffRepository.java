package com.example.demo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    // Тариф услуги, действующий на дату: последний с effectiveFrom <= date
    Optional<Tariff> findFirstByServiceTypeAndEffectiveFromLessThanEqualOrderByEffectiveFromDescIdDesc(
            String serviceType, LocalDate date);

    // История тарифов (сначала новые)
    List<Tariff> findAllByOrderByServiceTypeAscEffectiveFromDescIdDesc();

    boolean existsByServiceTypeAndEffectiveFrom(String serviceType, LocalDate effectiveFrom);
}

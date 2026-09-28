package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillRepository extends JpaRepository<Bill, Long> {

    // История начислений абонента, сначала самые новые
    List<Bill> findBySubscriberIdOrderByPeriodDescIdDesc(Long subscriberId);

    // Сумма всех неоплаченных начислений абонента
    @Query("select coalesce(sum(b.amount), 0) from Bill b where b.subscriber.id = :subscriberId and b.paid = false")
    double sumUnpaid(@Param("subscriberId") Long subscriberId);
}

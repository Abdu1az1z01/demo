package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    // Абоненты одной услуги; поиск по ФИО, лицевому счёту или адресу (без учёта регистра)
    @Query("""
            select s from Subscriber s
            where s.serviceType = :service
              and (:search = ''
                   or lower(s.ownerName) like lower(concat('%', :search, '%'))
                   or s.accountNumber like concat('%', :search, '%')
                   or lower(s.address) like lower(concat('%', :search, '%')))
            order by s.ownerName
            """)
    List<Subscriber> search(@Param("service") String service, @Param("search") String search);
}

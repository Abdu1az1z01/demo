package com.example.demo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {

    // Поиск абонентов по ФИО, лицевому счёту, телефону или адресу (без учёта регистра).
    // service = null — искать во всех услугах сразу.
    @Query("""
            select s from Subscriber s
            where (:service is null or s.serviceType = :service)
              and (:search = ''
                   or lower(s.ownerName) like lower(concat('%', :search, '%'))
                   or s.accountNumber like concat('%', :search, '%')
                   or s.phone like concat('%', :search, '%')
                   or lower(s.address) like lower(concat('%', :search, '%')))
            order by s.ownerName
            """)
    List<Subscriber> search(@Param("service") String service, @Param("search") String search);

    Optional<Subscriber> findByAccountNumberAndServiceType(String accountNumber, String serviceType);

    boolean existsByAccountNumberAndServiceType(String accountNumber, String serviceType);

    boolean existsByAccountNumberAndServiceTypeAndIdNot(String accountNumber, String serviceType, Long id);
}

package com.equestrian.club.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthOpLedgerRepository extends JpaRepository<HealthOpLedger, Long> {

    Optional<HealthOpLedger> findByRequestKey(String requestKey);

    boolean existsByRequestKey(String requestKey);
}

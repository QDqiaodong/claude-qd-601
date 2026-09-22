package com.equestrian.club.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HorseRepository extends JpaRepository<Horse, Long> {

    List<Horse> findAllByOrderByIdAsc();

    Optional<Horse> findByHorseNo(String horseNo);

    boolean existsByHorseNo(String horseNo);
}

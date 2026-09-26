package com.equestrian.club.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface HorseRepository extends JpaRepository<Horse, Long> {

    List<Horse> findAllByOrderByIdAsc();

    Optional<Horse> findByHorseNo(String horseNo);

    boolean existsByHorseNo(String horseNo);

    /**
     * 行级悲观锁：健康事件登记 / 流转 / 放行都先锁马行再改状态，
     * 保证同一匹马的并发操作串行化，不同马互不阻塞。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Horse h where h.id = :id")
    Optional<Horse> findByIdForUpdate(@Param("id") Long id);
}

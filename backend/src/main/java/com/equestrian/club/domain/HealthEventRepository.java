package com.equestrian.club.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthEventRepository extends JpaRepository<HealthEvent, Long> {

    Optional<HealthEvent> findByEventNo(String eventNo);

    List<HealthEvent> findAllByOrderByIdDesc();

    List<HealthEvent> findByHorseIdOrderByIdDesc(Long horseId);

    /** 某匹马全部未关闭事件（放行时逐一校验合格结论） */
    @Query("select e from HealthEvent e where e.horseId = :horseId "
            + "and e.status in ('PENDING', 'OBSERVING', 'REVIEW_PENDING') order by e.id asc")
    List<HealthEvent> findOpenByHorseId(@Param("horseId") Long horseId);

    /** 列表筛选：马匹 + 状态（状态为空时不过滤） */
    List<HealthEvent> findByHorseIdAndStatusInOrderByIdDesc(Long horseId, List<String> statuses);

    List<HealthEvent> findByStatusInOrderByIdDesc(List<String> statuses);

    /** 某匹马最新的一条事件（用来判断放行结论是否基于旧事件链） */
    Optional<HealthEvent> findFirstByHorseIdOrderByIdDesc(Long horseId);

    long countByHorseIdAndStatusIn(Long horseId, List<String> statuses);
}

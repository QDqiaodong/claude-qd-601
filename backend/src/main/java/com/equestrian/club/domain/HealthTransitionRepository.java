package com.equestrian.club.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthTransitionRepository extends JpaRepository<HealthTransition, Long> {

    List<HealthTransition> findByEventIdOrderByIdAsc(Long eventId);

    List<HealthTransition> findByHorseIdOrderByIdAsc(Long horseId);

    /** 马匹事件链版本 = 该马最新一条流转历史的流水号；没有任何历史时为 0 */
    Optional<HealthTransition> findFirstByHorseIdOrderByIdDesc(Long horseId);

    /** 取某匹马最近一次「复查合格」结论（马匹详情里的最近复查结论） */
    Optional<HealthTransition> findFirstByHorseIdAndActionOrderByIdDesc(Long horseId, String action);
}

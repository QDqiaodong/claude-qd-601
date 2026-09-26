package com.equestrian.club.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthEventSessionRepository extends JpaRepository<HealthEventSession, Long> {

    List<HealthEventSession> findByEventIdOrderByIdAsc(Long eventId);

    List<HealthEventSession> findBySessionIdOrderByIdAsc(Long sessionId);

    boolean existsByEventIdAndSessionId(Long eventId, Long sessionId);

    /** 某匹马被追溯过的全部排期 id（受影响标记的来源） */
    @Query("select distinct h.sessionId from HealthEventSession h where h.horseId = :horseId")
    List<Long> findSessionIdsByHorseId(@Param("horseId") Long horseId);

    /**
     * 某条排期是否仍被这匹马的未关闭高风险事件关联。
     * 放行（事件全部关闭）后返回 false，排期标记可以复位；历史关联行仍保留。
     */
    @Query("select count(hes) > 0 from HealthEventSession hes, HealthEvent e "
            + "where hes.sessionId = :sessionId and hes.eventId = e.id "
            + "and e.status in ('PENDING', 'OBSERVING', 'REVIEW_PENDING')")
    boolean isSessionStillAffected(@Param("sessionId") Long sessionId);
}

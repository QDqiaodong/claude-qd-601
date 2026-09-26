package com.equestrian.club.domain;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonSessionRepository extends JpaRepository<LessonSession, Long> {

    List<LessonSession> findAllByOrderBySessionDateAscStartTimeAscIdAsc();

    /** 某一天某位教练的全部排期（同一教练同一时段不可重叠） */
    List<LessonSession> findByCoachIdAndSessionDate(Long coachId, LocalDate sessionDate);

    /** 训练日历：某匹马在某段日期内的排期 */
    List<LessonSession> findByHorseIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            Long horseId, LocalDate from, LocalDate to);

    /**
     * 登记高风险事件时圈定受影响排期：该马、日期不早于今天、未取消的全部场次。
     * 只标记不删除（取消状态不动，历史排期不动）。
     */
    List<LessonSession> findByHorseIdAndSessionDateGreaterThanEqualAndStatusNotOrderBySessionDateAscStartTimeAsc(
            Long horseId, LocalDate from, String excludedStatus);

    /** 按一批排期主键取场次（放行复位受影响标记时用） */
    List<LessonSession> findByHorseIdAndIdIn(Long horseId, List<Long> ids);

    /** 训练日历：某段日期内的全部排期 */
    List<LessonSession> findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            LocalDate from, LocalDate to);

    long countByLessonId(Long lessonId);
}

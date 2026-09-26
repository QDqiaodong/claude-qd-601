package com.equestrian.club.dto.view;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 受健康事件影响的排期（追溯用）：从 health_event_session + lesson_session 组装。
 * 排期在健康事件详情里可回溯，但永远不会被健康流程删除。
 *
 * @param stillAffected 该场是否仍被未关闭事件关联（放行后历史行还在，但已不再受影响）
 */
public record AffectedSessionView(
        Long linkId,
        Long eventId,
        Long sessionId,
        String lessonName,
        String coachName,
        LocalDate sessionDate,
        String startTime,
        String endTime,
        String status,
        String statusName,
        Integer bookedCount,
        Integer capacity,
        Boolean stillAffected,
        LocalDateTime markedAt) {
}

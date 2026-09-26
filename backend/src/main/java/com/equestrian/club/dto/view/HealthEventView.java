package com.equestrian.club.dto.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康事件详情视图：事件当前快照 + append-only 流转历史 + 受影响排期追溯。
 *
 * @param overdue            未关闭且预计复查日已过（今天 > nextReviewDate）
 * @param passExpired        最近一次合格复查结论已过有效期
 * @param chainVersion       该马当前事件链版本（最新一条流转流水号），提交时回传
 * @param latestEventId      该马最新事件 id（判断当前结论是否基于旧事件链）
 */
public record HealthEventView(
        Long id,
        String eventNo,
        Long horseId,
        String horseNo,
        String horseName,
        String horseStatus,
        String horseStatusName,
        LocalDateTime occurredAt,
        String severity,
        String severityName,
        String symptom,
        String treatmentAdvice,
        String status,
        String statusName,
        LocalDate nextReviewDate,
        String passConclusion,
        LocalDateTime passReviewAt,
        Integer passValidDays,
        Boolean passExpired,
        Boolean overdue,
        LocalDateTime closedAt,
        Long version,
        Long chainVersion,
        Long latestEventId,
        LocalDateTime createdAt,
        List<HealthTransitionView> transitions,
        List<AffectedSessionView> affectedSessions) {
}

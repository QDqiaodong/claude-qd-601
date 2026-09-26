package com.equestrian.club.dto.view;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康事件流转历史视图：一条 append-only 记录。
 */
public record HealthTransitionView(
        Long id,
        Long eventId,
        Long horseId,
        String action,
        String actionName,
        String fromStatus,
        String fromStatusName,
        String toStatus,
        String toStatusName,
        String note,
        LocalDate nextReviewDate,
        String operatorName,
        String operatorRole,
        String operatorRoleName,
        Long chainVersion,
        LocalDateTime operatedAt) {
}

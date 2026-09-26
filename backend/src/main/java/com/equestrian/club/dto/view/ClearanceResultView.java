package com.equestrian.club.dto.view;

/**
 * 复训放行结果视图：放行通过后返回马匹最新状态、被关闭的事件数与最新事件链版本，
 * 前端据此刷新马匹卡片墙、排期页与健康事件列表。
 */
public record ClearanceResultView(
        Long horseId,
        String horseNo,
        String horseName,
        String horseStatus,
        String horseStatusName,
        int closedEventCount,
        Long chainVersion,
        String message) {
}

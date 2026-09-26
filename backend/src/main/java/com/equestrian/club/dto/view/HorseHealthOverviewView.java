package com.equestrian.club.dto.view;

import java.util.List;

/**
 * 马匹健康概览：马匹详情页一屏看清「还有哪些没闭环的事件 + 最近一次复查结论」。
 *
 * @param openEvents          仍未闭环的事件（含合格待放行）
 * @param latestPass          最近一次复查合格结论（可能来自已关闭的旧事件）
 * @param clearanceBlockers   当前为什么不能放行（空列表代表满足放行的健康条件）
 * @param canClear            负责人视角下此刻是否可放行
 * @param chainVersion        当前马匹事件链版本
 */
public record HorseHealthOverviewView(
        Long horseId,
        String horseNo,
        String horseName,
        String horseStatus,
        String horseStatusName,
        long openCount,
        long overdueCount,
        Long chainVersion,
        HealthEventView latestPass,
        List<HealthEventView> openEvents,
        List<String> clearanceBlockers,
        boolean canClear) {
}

package com.equestrian.club.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 马匹档案视图：基础信息（horse 主表）+ 健康档案（horse_health 副表）拼成一条返回。
 *
 * <p>健康事件闭环上线后追加末尾几个字段，马匹卡片墙不用翻页就能看到当前风险：
 * 未闭环事件数、逾期事件数、最近一次复查合格结论及其时间。
 */
public record HorseView(
        Long id,
        String horseNo,
        String name,
        String breed,
        String gender,
        Integer birthYear,
        Integer age,
        String status,
        String statusName,
        String rideLevel,
        String rideLevelName,
        LocalDate lastCheckDate,
        Integer vaccineCount,
        BigDecimal weightKg,
        String healthNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // ---- 健康事件闭环汇总（列表接口逐马轻量装配） ----
        Long openHealthEventCount,
        Long overdueHealthEventCount,
        LocalDate nearestReviewDate,
        String latestPassConclusion,
        LocalDateTime latestPassAt) {
}

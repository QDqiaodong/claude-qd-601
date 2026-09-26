package com.equestrian.club.dto.view;

import java.time.LocalDate;

/**
 * 课程排期视图：展开课程 / 教练 / 马匹的名称，并给出剩余名额。
 *
 * @param healthAffected 该场是否被某匹马的未关闭高风险事件标记为受影响（只标记，不删除排期）
 */
public record SessionView(
        Long id,
        Long lessonId,
        String lessonNo,
        String lessonName,
        String category,
        String categoryName,
        Long coachId,
        String coachName,
        Long horseId,
        String horseNo,
        String horseName,
        LocalDate sessionDate,
        String startTime,
        String endTime,
        Integer capacity,
        Integer bookedCount,
        Integer remain,
        String status,
        String statusName,
        Boolean healthAffected) {
}

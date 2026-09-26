package com.equestrian.club.dict;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 马术俱乐部业务字典与规则参数表。
 * 状态取值、状态机许可边、时段表、会员折扣等规则集中在此，便于按题面调整或扩展。
 */
public final class EquestrianDict {

    private EquestrianDict() {
    }

    // ---------------- 马匹在役状态机 ----------------
    public static final String HORSE_ACTIVE = "ACTIVE";
    public static final String HORSE_RESTING = "RESTING";
    public static final String HORSE_RETIRED = "RETIRED";

    private static final Map<String, String> HORSE_STATUS = Map.of(
            HORSE_ACTIVE, "在役",
            HORSE_RESTING, "休养",
            HORSE_RETIRED, "退役");

    /** 状态机许可边：退役是终态，不可回退 */
    private static final Map<String, List<String>> HORSE_STATUS_EDGE = Map.of(
            HORSE_ACTIVE, List.of(HORSE_RESTING, HORSE_RETIRED),
            HORSE_RESTING, List.of(HORSE_ACTIVE, HORSE_RETIRED),
            HORSE_RETIRED, List.of());

    // ---------------- 马匹骑乘等级 ----------------
    private static final Map<String, String> RIDE_LEVEL = Map.of(
            "BEGINNER_SAFE", "初级安全马",
            "INTERMEDIATE", "中级马",
            "ADVANCED", "高级马");

    // ---------------- 栏位状态 ----------------
    public static final String STALL_IDLE = "IDLE";
    public static final String STALL_OCCUPIED = "OCCUPIED";
    public static final String STALL_MAINTENANCE = "MAINTENANCE";

    private static final Map<String, String> STALL_STATUS = Map.of(
            STALL_IDLE, "空闲",
            STALL_OCCUPIED, "占用",
            STALL_MAINTENANCE, "维护中");

    // ---------------- 教练 ----------------
    private static final Map<String, String> COACH_LEVEL = Map.of(
            "JUNIOR", "初级教练",
            "SENIOR", "高级教练",
            "CHIEF", "总教练");

    public static final String COACH_ON_DUTY = "ON_DUTY";

    private static final Map<String, String> COACH_STATUS = Map.of(
            COACH_ON_DUTY, "在岗",
            "LEAVE", "休假");

    // ---------------- 课程 ----------------
    public static final String CATEGORY_BASIC = "BASIC";
    public static final String CATEGORY_ADVANCED = "ADVANCED";
    public static final String CATEGORY_PRIVATE = "PRIVATE";

    private static final Map<String, String> LESSON_CATEGORY = Map.of(
            CATEGORY_BASIC, "初级课",
            CATEGORY_ADVANCED, "进阶课",
            CATEGORY_PRIVATE, "私教课");

    public static final String LESSON_ON = "ON";

    private static final Map<String, String> LESSON_STATUS = Map.of(
            LESSON_ON, "上架",
            "OFF", "下架");

    // ---------------- 排期状态 ----------------
    public static final String SESSION_SCHEDULED = "SCHEDULED";
    public static final String SESSION_FULL = "FULL";
    public static final String SESSION_CANCELED = "CANCELED";

    private static final Map<String, String> SESSION_STATUS = Map.of(
            SESSION_SCHEDULED, "已排期",
            SESSION_FULL, "已满员",
            SESSION_CANCELED, "已取消");

    // ---------------- 会员 ----------------
    private static final Map<String, String> MEMBER_LEVEL = Map.of(
            "NORMAL", "普通会员",
            "SILVER", "银卡会员",
            "GOLD", "金卡会员",
            "DIAMOND", "钻石卡会员");

    private static final Map<String, BigDecimal> MEMBER_DISCOUNT = Map.of(
            "NORMAL", BigDecimal.valueOf(1.00),
            "SILVER", BigDecimal.valueOf(0.95),
            "GOLD", BigDecimal.valueOf(0.90),
            "DIAMOND", BigDecimal.valueOf(0.80));

    public static final String MEMBER_NORMAL = "NORMAL";

    private static final Map<String, String> MEMBER_STATUS = Map.of(
            MEMBER_NORMAL, "正常",
            "FROZEN", "冻结");

    // ---------------- 骑乘记录状态 ----------------
    public static final String RECORD_BOOKED = "BOOKED";
    public static final String RECORD_COMPLETED = "COMPLETED";
    public static final String RECORD_CANCELED = "CANCELED";

    private static final Map<String, String> RECORD_STATUS = Map.of(
            RECORD_BOOKED, "已预约",
            RECORD_COMPLETED, "已完成",
            RECORD_CANCELED, "已取消");

    // ---------------- 健康事件严重程度 ----------------
    public static final String SEVERITY_LOW = "LOW";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_HIGH = "HIGH";

    private static final Map<String, String> HEALTH_SEVERITY = Map.of(
            SEVERITY_LOW, "低风险",
            SEVERITY_MEDIUM, "中风险",
            SEVERITY_HIGH, "高风险");

    // ---------------- 健康事件状态机 ----------------
    public static final String EVENT_PENDING = "PENDING";
    public static final String EVENT_OBSERVING = "OBSERVING";
    public static final String EVENT_REVIEW_PENDING = "REVIEW_PENDING";
    public static final String EVENT_CLOSED = "CLOSED";

    /** 未关闭（仍在事件链上、放行时必须全部合格）的状态集合 */
    public static final List<String> EVENT_OPEN_STATUSES =
            List.of(EVENT_PENDING, EVENT_OBSERVING, EVENT_REVIEW_PENDING);

    private static final Map<String, String> HEALTH_EVENT_STATUS = Map.of(
            EVENT_PENDING, "待处理",
            EVENT_OBSERVING, "观察中",
            EVENT_REVIEW_PENDING, "待复查",
            EVENT_CLOSED, "已关闭");

    // ---------------- 健康事件流转动作 ----------------
    /** 登记 */
    public static final String ACTION_REGISTER = "REGISTER";
    /** 补充处置（状态不变，只追加历史） */
    public static final String ACTION_PROCESS = "PROCESS";
    /** 开始观察：待处理 -> 观察中 */
    public static final String ACTION_START_OBSERVE = "START_OBSERVE";
    /** 申请复查：观察中 -> 待复查 */
    public static final String ACTION_REQUEST_REVIEW = "REQUEST_REVIEW";
    /** 复查后继续观察：待复查 -> 观察中（可同时调整下次复查日） */
    public static final String ACTION_REVIEW_CONTINUE = "REVIEW_CONTINUE";
    /** 复查合格：待复查维持待复查，写入合格结论，等待负责人放行 */
    public static final String ACTION_REVIEW_PASS = "REVIEW_PASS";
    /** 关闭：复训放行通过后由系统逐事件写入 */
    public static final String ACTION_CLOSE = "CLOSE";

    private static final Map<String, String> HEALTH_ACTION = Map.of(
            ACTION_REGISTER, "登记事件",
            ACTION_PROCESS, "补充处置",
            ACTION_START_OBSERVE, "开始观察",
            ACTION_REQUEST_REVIEW, "申请复查",
            ACTION_REVIEW_CONTINUE, "复查后继续观察",
            ACTION_REVIEW_PASS, "复查合格",
            ACTION_CLOSE, "复训放行关闭");

    /** 各动作允许的来源状态（登记不校验来源） */
    private static final Map<String, List<String>> HEALTH_ACTION_FROM = Map.of(
            ACTION_PROCESS, List.of(EVENT_PENDING, EVENT_OBSERVING, EVENT_REVIEW_PENDING),
            ACTION_START_OBSERVE, List.of(EVENT_PENDING),
            ACTION_REQUEST_REVIEW, List.of(EVENT_OBSERVING),
            ACTION_REVIEW_CONTINUE, List.of(EVENT_REVIEW_PENDING),
            ACTION_REVIEW_PASS, List.of(EVENT_REVIEW_PENDING));

    /** 合格复查结论的有效天数：放行时结论超过该天数即视为复查过期，必须重新复查 */
    public static final int PASS_VALID_DAYS = 3;

    // ---------------- 健康处置操作人角色 ----------------
    /** 普通工作人员：可登记事件、补充处置、推进复查 */
    public static final String ROLE_STAFF = "STAFF";
    /** 负责人：在普通工作人员权限之外，唯一能确认复训放行的角色 */
    public static final String ROLE_MANAGER = "MANAGER";

    private static final Map<String, String> OPERATOR_ROLE = Map.of(
            ROLE_STAFF, "普通工作人员",
            ROLE_MANAGER, "负责人");

    // ---------------- 训练日历时段 ----------------
    /** 训练日历固定时段起点（整点，每次一小时） */
    public static final List<String> TIME_SLOTS = List.of(
            "09:00", "10:00", "11:00", "14:00", "15:00", "16:00");

    /** 日历默认展示天数 */
    public static final int CALENDAR_DAYS = 7;

    /** 日历最多展示天数 */
    public static final int CALENDAR_MAX_DAYS = 14;

    // ---------------- 取值方法 ----------------

    public static boolean isValidHorseStatus(String status) {
        return status != null && HORSE_STATUS.containsKey(status);
    }

    public static String horseStatusName(String status) {
        return status == null ? "" : HORSE_STATUS.getOrDefault(status, status);
    }

    public static List<String> horseStatusTargets(String status) {
        return HORSE_STATUS_EDGE.getOrDefault(status, List.of());
    }

    public static boolean canTransferHorseStatus(String from, String to) {
        return horseStatusTargets(from).contains(to);
    }

    public static boolean isValidRideLevel(String level) {
        return level != null && RIDE_LEVEL.containsKey(level);
    }

    public static String rideLevelName(String level) {
        return level == null ? "" : RIDE_LEVEL.getOrDefault(level, level);
    }

    public static boolean isValidStallStatus(String status) {
        return status != null && STALL_STATUS.containsKey(status);
    }

    public static String stallStatusName(String status) {
        return status == null ? "" : STALL_STATUS.getOrDefault(status, status);
    }

    public static boolean isValidCoachStatus(String status) {
        return status != null && COACH_STATUS.containsKey(status);
    }

    public static String coachStatusName(String status) {
        return status == null ? "" : COACH_STATUS.getOrDefault(status, status);
    }

    public static String coachLevelName(String level) {
        return level == null ? "" : COACH_LEVEL.getOrDefault(level, level);
    }

    public static boolean isValidCategory(String category) {
        return category != null && LESSON_CATEGORY.containsKey(category);
    }

    public static String categoryName(String category) {
        return category == null ? "" : LESSON_CATEGORY.getOrDefault(category, category);
    }

    public static boolean isAdvancedCategory(String category) {
        return CATEGORY_ADVANCED.equals(category) || CATEGORY_PRIVATE.equals(category);
    }

    public static boolean isValidLessonStatus(String status) {
        return status != null && LESSON_STATUS.containsKey(status);
    }

    public static String lessonStatusName(String status) {
        return status == null ? "" : LESSON_STATUS.getOrDefault(status, status);
    }

    public static boolean isValidSessionStatus(String status) {
        return status != null && SESSION_STATUS.containsKey(status);
    }

    public static String sessionStatusName(String status) {
        return status == null ? "" : SESSION_STATUS.getOrDefault(status, status);
    }

    public static boolean isValidMemberLevel(String level) {
        return level != null && MEMBER_LEVEL.containsKey(level);
    }

    public static String memberLevelName(String level) {
        return level == null ? "" : MEMBER_LEVEL.getOrDefault(level, level);
    }

    public static BigDecimal memberDiscount(String level) {
        return MEMBER_DISCOUNT.getOrDefault(level, BigDecimal.valueOf(1.00));
    }

    public static boolean isValidMemberStatus(String status) {
        return status != null && MEMBER_STATUS.containsKey(status);
    }

    public static String memberStatusName(String status) {
        return status == null ? "" : MEMBER_STATUS.getOrDefault(status, status);
    }

    public static boolean isValidRecordStatus(String status) {
        return status != null && RECORD_STATUS.containsKey(status);
    }

    public static String recordStatusName(String status) {
        return status == null ? "" : RECORD_STATUS.getOrDefault(status, status);
    }

    // ---------------- 健康事件取值方法 ----------------

    public static boolean isValidSeverity(String severity) {
        return severity != null && HEALTH_SEVERITY.containsKey(severity);
    }

    public static String severityName(String severity) {
        return severity == null ? "" : HEALTH_SEVERITY.getOrDefault(severity, severity);
    }

    public static boolean isHighRisk(String severity) {
        return SEVERITY_HIGH.equals(severity);
    }

    public static boolean isValidHealthEventStatus(String status) {
        return status != null && HEALTH_EVENT_STATUS.containsKey(status);
    }

    public static String healthEventStatusName(String status) {
        return status == null ? "" : HEALTH_EVENT_STATUS.getOrDefault(status, status);
    }

    public static boolean isHealthEventOpen(String status) {
        return status != null && EVENT_OPEN_STATUSES.contains(status);
    }

    public static boolean isValidHealthAction(String action) {
        return action != null && HEALTH_ACTION.containsKey(action);
    }

    public static String healthActionName(String action) {
        return action == null ? "" : HEALTH_ACTION.getOrDefault(action, action);
    }

    /** 某动作是否允许从 fromStatus 发起（登记动作没有来源状态约束） */
    public static boolean canHealthAction(String action, String fromStatus) {
        if (ACTION_REGISTER.equals(action) || ACTION_CLOSE.equals(action)) {
            return true;
        }
        return HEALTH_ACTION_FROM.getOrDefault(action, List.of()).contains(fromStatus);
    }

    /** 动作执行后的事件状态；CLOSE 由放行流程直接给目标态，不在此表 */
    public static String healthActionTarget(String action, String current) {
        return switch (action) {
            case ACTION_REGISTER -> EVENT_PENDING;
            case ACTION_PROCESS -> current;
            case ACTION_START_OBSERVE -> EVENT_OBSERVING;
            case ACTION_REQUEST_REVIEW -> EVENT_REVIEW_PENDING;
            case ACTION_REVIEW_CONTINUE -> EVENT_OBSERVING;
            // 合格复查不关闭事件：休养马的事件维持「待复查」，等负责人放行时统一关闭
            case ACTION_REVIEW_PASS -> EVENT_REVIEW_PENDING;
            default -> current;
        };
    }

    public static boolean isValidOperatorRole(String role) {
        return role != null && OPERATOR_ROLE.containsKey(role);
    }

    public static String operatorRoleName(String role) {
        return role == null ? "" : OPERATOR_ROLE.getOrDefault(role, role);
    }

    /** 只有负责人能确认复训放行；普通工作人员的权限止于登记、补充处置与复查 */
    public static boolean canClearHealth(String role) {
        return ROLE_MANAGER.equals(role);
    }

    public static boolean isValidTimeSlot(String startTime) {
        return startTime != null && TIME_SLOTS.contains(startTime);
    }

    /**
     * 依据整点起始时间推出一小时后的结束时间，如 09:00 -> 10:00
     */
    public static String endOfSlot(String startTime) {
        if (startTime == null || startTime.length() != 5) {
            return null;
        }
        int hour;
        try {
            hour = Integer.parseInt(startTime.substring(0, 2));
        } catch (NumberFormatException e) {
            return null;
        }
        return String.format("%02d:%s", (hour + 1) % 24, startTime.substring(3));
    }

    /**
     * 判断两个 [start, end) 时段是否重叠（同一天的 HH:mm 字符串直接比较即可）
     */
    public static boolean overlap(String startA, String endA, String startB, String endB) {
        if (startA == null || endA == null || startB == null || endB == null) {
            return false;
        }
        return startA.compareTo(endB) < 0 && endA.compareTo(startB) > 0;
    }
}

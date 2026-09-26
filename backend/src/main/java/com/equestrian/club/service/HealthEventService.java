package com.equestrian.club.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equestrian.club.common.BizException;
import com.equestrian.club.common.ConflictException;
import com.equestrian.club.common.ForbiddenException;
import com.equestrian.club.dict.EquestrianDict;
import com.equestrian.club.domain.Coach;
import com.equestrian.club.domain.CoachRepository;
import com.equestrian.club.domain.HealthEvent;
import com.equestrian.club.domain.HealthEventRepository;
import com.equestrian.club.domain.HealthEventSession;
import com.equestrian.club.domain.HealthEventSessionRepository;
import com.equestrian.club.domain.HealthOpLedger;
import com.equestrian.club.domain.HealthOpLedgerRepository;
import com.equestrian.club.domain.HealthTransition;
import com.equestrian.club.domain.HealthTransitionRepository;
import com.equestrian.club.domain.Horse;
import com.equestrian.club.domain.HorseRepository;
import com.equestrian.club.domain.Lesson;
import com.equestrian.club.domain.LessonRepository;
import com.equestrian.club.domain.LessonSession;
import com.equestrian.club.domain.LessonSessionRepository;
import com.equestrian.club.dto.HealthClearRequest;
import com.equestrian.club.dto.HealthEventCreateRequest;
import com.equestrian.club.dto.HealthTransitionRequest;
import com.equestrian.club.dto.view.AffectedSessionView;
import com.equestrian.club.dto.view.ClearanceResultView;
import com.equestrian.club.dto.view.HealthEventView;
import com.equestrian.club.dto.view.HealthTransitionView;
import com.equestrian.club.dto.view.HorseHealthOverviewView;

/**
 * 马匹健康事件闭环核心服务。
 *
 * <p>一致性口径（成功 / 拒绝 / 并发都成立）：
 * <ul>
 *   <li>登记 HIGH 高风险事件：锁马行 → 马匹立即 RESTING（已是休养则不变、退役马保持退役）
 *       → 未来未取消用马排期打 health_affected 并落追溯表，绝不删除排期。</li>
 *   <li>每次流转只向 horse_health_transition 追加历史，不更新 / 删除既有行。</li>
 *   <li>复训放行是马匹级、负责人专属动作：所有未关闭事件都拿到在有效期内的合格复查，
 *       且没有更晚的新事件，马匹处于休养，才恢复在役并逐事件关闭；任一不满足都拒绝并给出具体原因。</li>
 *   <li>并发：事件乐观锁版本 + 马匹事件链版本双重校验，旧页面提交一律 409；
 *       request_key 幂等键保证重复点击不产生第二条历史、不重复改变马匹状态。</li>
 * </ul>
 */
@Service
public class HealthEventService {

    private final HealthEventRepository eventRepository;
    private final HealthTransitionRepository transitionRepository;
    private final HealthEventSessionRepository eventSessionRepository;
    private final HealthOpLedgerRepository ledgerRepository;
    private final HorseRepository horseRepository;
    private final LessonSessionRepository sessionRepository;
    private final LessonRepository lessonRepository;
    private final CoachRepository coachRepository;
    private final HealthIdempotencyService idempotency;

    public HealthEventService(HealthEventRepository eventRepository,
            HealthTransitionRepository transitionRepository,
            HealthEventSessionRepository eventSessionRepository,
            HealthOpLedgerRepository ledgerRepository,
            HorseRepository horseRepository,
            LessonSessionRepository sessionRepository,
            LessonRepository lessonRepository,
            CoachRepository coachRepository,
            HealthIdempotencyService idempotency) {
        this.eventRepository = eventRepository;
        this.transitionRepository = transitionRepository;
        this.eventSessionRepository = eventSessionRepository;
        this.ledgerRepository = ledgerRepository;
        this.horseRepository = horseRepository;
        this.sessionRepository = sessionRepository;
        this.lessonRepository = lessonRepository;
        this.coachRepository = coachRepository;
        this.idempotency = idempotency;
    }

    // ============================ 查询 ============================

    /** 事件列表：可按马匹、状态筛选；overdueOnly 只看未关闭且复查已逾期的事件 */
    @Transactional(readOnly = true)
    public List<HealthEventView> list(Long horseId, String status, Boolean overdueOnly) {
        List<HealthEvent> events;
        boolean openOnly = status != null && !status.isBlank();
        if (horseId != null && openOnly) {
            if (!EquestrianDict.isValidHealthEventStatus(status)) {
                throw new BizException("事件状态不合法：" + status);
            }
            events = eventRepository.findByHorseIdAndStatusInOrderByIdDesc(horseId, List.of(status));
        } else if (horseId != null) {
            events = eventRepository.findByHorseIdOrderByIdDesc(horseId);
        } else if (openOnly) {
            if (!EquestrianDict.isValidHealthEventStatus(status)) {
                throw new BizException("事件状态不合法：" + status);
            }
            events = eventRepository.findByStatusInOrderByIdDesc(List.of(status));
        } else {
            events = eventRepository.findAllByOrderByIdDesc();
        }

        LocalDate today = LocalDate.now();
        return events.stream()
                .filter((event) -> overdueOnly == null || !overdueOnly || isOverdue(event, today))
                .map((event) -> toView(event, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public HealthEventView detail(Long eventId) {
        return toView(requireEvent(eventId), true);
    }

    /**
     * 马匹健康概览：未闭环事件清单 + 最近一次合格复查结论 + 此刻能否放行 / 不能放行的原因。
     */
    @Transactional(readOnly = true)
    public HorseHealthOverviewView overview(Long horseId, String operatorRole) {
        Horse horse = requireHorse(horseId);
        LocalDate today = LocalDate.now();

        List<HealthEvent> openEvents = eventRepository.findOpenByHorseId(horseId);
        List<HealthEventView> openViews = new ArrayList<>();
        long overdueCount = 0;
        for (HealthEvent event : openEvents) {
            openViews.add(toView(event, false));
            if (isOverdue(event, today)) {
                overdueCount++;
            }
        }

        HealthEventView latestPass = transitionRepository
                .findFirstByHorseIdAndActionOrderByIdDesc(horseId, EquestrianDict.ACTION_REVIEW_PASS)
                .map((transition) -> eventRepository.findById(transition.getEventId()).orElse(null))
                .map((event) -> event == null ? null : toView(event, false))
                .orElse(null);

        List<String> blockers = clearanceBlockers(horse, openEvents, today);
        boolean manager = EquestrianDict.ROLE_MANAGER.equals(operatorRole);

        return new HorseHealthOverviewView(
                horse.getId(),
                horse.getHorseNo(),
                horse.getName(),
                horse.getStatus(),
                EquestrianDict.horseStatusName(horse.getStatus()),
                openEvents.size(),
                overdueCount,
                chainVersionOf(horseId),
                latestPass,
                openViews,
                blockers,
                manager && blockers.isEmpty());
    }

    // ============================ 登记事件 ============================

    @Transactional
    public HealthEventView register(HealthEventCreateRequest request) {
        if (request == null) {
            throw new BizException("请求体不能为空");
        }
        String role = idempotency.requireValidRole(request.getOperatorRole());
        Long horseId = request.getHorseId();
        String operatorName = trimRequired(request.getOperatorName(), "操作人姓名");

        // 先做全部参数校验，避免校验失败时留下未完成的幂等键
        LocalDateTime occurredAt = request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt();
        if (occurredAt.isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new BizException("发生时间不能晚于当前时间");
        }
        String severity = request.getSeverity();
        if (!EquestrianDict.isValidSeverity(severity)) {
            throw new BizException("严重程度不合法：" + severity + "，只支持 LOW / MEDIUM / HIGH");
        }
        String symptom = trimRequired(request.getSymptom(), "症状说明");
        String advice = trimRequired(request.getTreatmentAdvice(), "处置建议");
        LocalDate nextReviewDate = request.getNextReviewDate();
        if (nextReviewDate == null) {
            throw new BizException("预计复查日不能为空");
        }
        if (nextReviewDate.isBefore(LocalDate.now())) {
            throw new BizException("预计复查日不能早于登记当天");
        }

        Horse horse = lockHorse(horseId);

        // 参数都合法后再占幂等键（锁马行），命中则重放首次登记结果
        Optional<HealthOpLedger> existing = idempotency.acquire(
                request.getRequestKey(), HealthIdempotencyService.SCOPE_EVENT, horseId, operatorName);
        if (existing.isPresent() && existing.get().getEventId() != null) {
            return toView(requireEvent(existing.get().getEventId()), true);
        }

        HealthEvent event = new HealthEvent();
        event.setEventNo(generateEventNo());
        event.setHorseId(horse.getId());
        event.setOccurredAt(occurredAt);
        event.setSeverity(severity);
        event.setSymptom(symptom);
        event.setTreatmentAdvice(advice);
        event.setStatus(EquestrianDict.EVENT_PENDING);
        event.setNextReviewDate(nextReviewDate);
        event = eventRepository.saveAndFlush(event);

        // 登记历史先落一条，确定事件链版本
        appendTransition(event, EquestrianDict.ACTION_REGISTER, null,
                EquestrianDict.EVENT_PENDING, advice, nextReviewDate, operatorName, role, 0L);

        boolean highRisk = EquestrianDict.isHighRisk(severity);
        List<LessonSession> affected = List.of();
        if (highRisk) {
            // 高风险：在役马立即休养；已休养保持休养；退役是终态，不改变其退役状态
            if (EquestrianDict.HORSE_ACTIVE.equals(horse.getStatus())) {
                horse.setStatus(EquestrianDict.HORSE_RESTING);
                horseRepository.save(horse);
            }
            affected = markFutureSessionsAffected(event);
        }

        idempotency.bindEvent(request.getRequestKey(), event.getId());
        return toView(event, true);
    }

    // ============================ 事件流转 ============================

    @Transactional
    public HealthEventView transition(Long eventId, HealthTransitionRequest request) {
        if (request == null) {
            throw new BizException("请求体不能为空");
        }
        String role = idempotency.requireValidRole(request.getOperatorRole());
        String operatorName = trimRequired(request.getOperatorName(), "操作人姓名");

        // 先确认事件并锁马行
        HealthEvent event = requireEvent(eventId);
        Horse horse = lockHorse(event.getHorseId());

        String action = request.getAction();
        if (!EquestrianDict.isValidHealthAction(action)
                || EquestrianDict.ACTION_REGISTER.equals(action)
                || EquestrianDict.ACTION_CLOSE.equals(action)) {
            throw new BizException("流转动作不合法：" + action);
        }

        String current = event.getStatus();
        if (!EquestrianDict.canHealthAction(action, current)) {
            throw new BizException("事件「" + event.getEventNo() + "」当前为「"
                    + EquestrianDict.healthEventStatusName(current) + "」，不能执行「"
                    + EquestrianDict.healthActionName(action) + "」");
        }

        // 并发保护：页面打开后有人登记了新伤病 / 完成了新复查，链版本就会前进
        assertChainCurrent(horse.getId(), request.getChainVersion(), event);

        String note = request.getNote() == null ? null : request.getNote().trim();
        LocalDate nextReviewDate = request.getNextReviewDate();
        LocalDate today = LocalDate.now();
        String targetStatus = EquestrianDict.healthActionTarget(action, current);

        switch (action) {
            case EquestrianDict.ACTION_PROCESS:
                note = trimRequired(request.getNote(), "处置说明");
                break;
            case EquestrianDict.ACTION_START_OBSERVE:
                note = trimRequired(note, "观察说明");
                requireReviewDate(nextReviewDate, today, "开始观察时必须设定下次复查日");
                break;
            case EquestrianDict.ACTION_REQUEST_REVIEW:
                note = trimRequired(note, "复查申请说明");
                requireReviewDate(nextReviewDate, today, "申请复查必须给出计划复查日");
                break;
            case EquestrianDict.ACTION_REVIEW_CONTINUE:
                note = trimRequired(note, "复查说明");
                requireReviewDate(nextReviewDate, today, "继续观察必须设定新的下次复查日");
                break;
            case EquestrianDict.ACTION_REVIEW_PASS: {
                String conclusion = trimRequired(request.getConclusion(), "合格复查结论");
                requireReviewDate(nextReviewDate, today, "合格复查必须给出下次复查日（复训前跟踪用）");
                note = conclusion;
                break;
            }
            default:
                break;
        }

        // 全部校验（含并发版本）通过后才占幂等键，保证校验失败时不留半成品键
        Optional<HealthOpLedger> existing = idempotency.acquire(
                request.getRequestKey(), HealthIdempotencyService.SCOPE_EVENT,
                horse.getId(), operatorName);
        if (existing.isPresent()) {
            Long targetEventId = existing.get().getEventId() != null
                    ? existing.get().getEventId()
                    : eventId;
            return toView(requireEvent(targetEventId), true);
        }

        if (EquestrianDict.ACTION_REVIEW_PASS.equals(action)) {
            // 记录最近一次合格复查结论
            event.setPassConclusion(request.getConclusion().trim());
            event.setPassReviewAt(LocalDateTime.now());
            event.setPassValidDays(EquestrianDict.PASS_VALID_DAYS);

            // 马仍在役（LOW/MEDIUM 事件未导致休养）：复查合格即随复查关闭，不涉及复训放行；
            // 马在休养：事件保持「待复查」，必须等负责人复训放行时统一关闭。
            if (EquestrianDict.HORSE_ACTIVE.equals(horse.getStatus())) {
                targetStatus = EquestrianDict.EVENT_CLOSED;
                event.setStatus(EquestrianDict.EVENT_CLOSED);
                event.setClosedAt(LocalDateTime.now());
                event.setNextReviewDate(null);
            }
        }
        if (EquestrianDict.ACTION_REVIEW_CONTINUE.equals(action)) {
            // 继续观察意味着旧的合格结论不再代表当前状态
            event.setPassConclusion(null);
            event.setPassReviewAt(null);
            event.setPassValidDays(null);
        }
        if (!EquestrianDict.EVENT_CLOSED.equals(targetStatus)) {
            event.setNextReviewDate(nextReviewDate);
            event.setStatus(targetStatus);
        }
        event = eventRepository.saveAndFlush(event);

        appendTransition(event, action, current, targetStatus, note,
                EquestrianDict.EVENT_CLOSED.equals(targetStatus) ? null : nextReviewDate,
                operatorName, role, 0L);

        idempotency.bindEvent(request.getRequestKey(), event.getId());
        return toView(event, true);
    }

    // ============================ 复训放行（负责人专属） ============================

    @Transactional
    public ClearanceResultView clear(HealthClearRequest request) {
        if (request == null) {
            throw new BizException("请求体不能为空");
        }
        String role = idempotency.requireValidRole(request.getOperatorRole());
        String operatorName = trimRequired(request.getOperatorName(), "操作人姓名");
        Long horseId = request.getHorseId();
        String note = trimRequired(request.getNote(), "放行说明");

        // 权限：最终复训放行只能由负责人确认（后端硬校验，不依赖前端隐藏按钮）
        if (!EquestrianDict.canClearHealth(role)) {
            throw new ForbiddenException("复训放行只能由负责人确认，当前身份是「普通工作人员」");
        }

        Horse horse = lockHorse(horseId);

        // 并发：打开放行弹窗后又有人登记新伤病 / 完成复查，旧提交必须拒绝
        assertChainCurrent(horseId, request.getChainVersion(), null);

        LocalDate today = LocalDate.now();
        List<HealthEvent> openEvents = eventRepository.findOpenByHorseId(horseId);
        List<String> blockers = clearanceBlockers(horse, openEvents, today);

        // 全部条件校验通过后才占幂等键；命中说明这次放行已成功处理过，回放当前状态即可
        Optional<HealthOpLedger> existing = idempotency.acquire(
                request.getRequestKey(), HealthIdempotencyService.SCOPE_CLEAR, horseId, operatorName);
        if (existing.isPresent()) {
            int closed = (int) eventRepository.countByHorseIdAndStatusIn(
                    horseId, List.of(EquestrianDict.EVENT_CLOSED));
            return new ClearanceResultView(horse.getId(), horse.getHorseNo(), horse.getName(),
                    horse.getStatus(), EquestrianDict.horseStatusName(horse.getStatus()),
                    closed, chainVersionOf(horseId), "该放行请求已处理过，这是重复提交，状态未重复改变");
        }

        if (!blockers.isEmpty()) {
            throw new BizException("复训放行被拒绝：" + String.join("；", blockers));
        }

        // 全部未关闭事件均已合格：逐事件关闭并追加 CLOSE 历史
        int closed = 0;
        for (HealthEvent event : openEvents) {
            String from = event.getStatus();
            event.setStatus(EquestrianDict.EVENT_CLOSED);
            event.setClosedAt(LocalDateTime.now());
            event.setNextReviewDate(null);
            eventRepository.save(event);
            appendTransition(event, EquestrianDict.ACTION_CLOSE, from,
                    EquestrianDict.EVENT_CLOSED, note, null, operatorName, role, 0L);
            closed++;
        }

        // 恢复在役（状态机合法边：RESTING -> ACTIVE）；退役马已在 blockers 中被拦截
        horse.setStatus(EquestrianDict.HORSE_ACTIVE);
        horseRepository.save(horse);

        // 复位这些事件关联排期的受影响标记（追溯行保留）
        unmarkSessionsIfRecovered(horseId);

        return new ClearanceResultView(horse.getId(), horse.getHorseNo(), horse.getName(),
                horse.getStatus(), EquestrianDict.horseStatusName(horse.getStatus()),
                closed, chainVersionOf(horseId),
                "复训放行已确认，" + closed + " 条健康事件已关闭，马匹恢复在役");
    }

    // ============================ 核心规则 ============================

    /**
     * 计算放行阻塞原因。返回空列表代表满足放行的健康条件。
     * 覆盖：马匹退役 / 马匹不在休养 / 仍有未闭环事件 / 复查过期 / 结论基于旧事件。
     */
    private List<String> clearanceBlockers(Horse horse, List<HealthEvent> openEvents, LocalDate today) {
        List<String> blockers = new ArrayList<>();

        if (EquestrianDict.HORSE_RETIRED.equals(horse.getStatus())) {
            blockers.add("马匹「" + horse.getName() + "」已退役，退役为终态，不能复训恢复在役");
            return blockers;
        }
        if (!EquestrianDict.HORSE_RESTING.equals(horse.getStatus())) {
            blockers.add("马匹「" + horse.getName() + "」当前为「"
                    + EquestrianDict.horseStatusName(horse.getStatus()) + "」，无需复训放行");
        }

        if (openEvents.isEmpty()) {
            blockers.add("没有需要放行的未关闭健康事件");
            return blockers;
        }

        for (HealthEvent event : openEvents) {
            String prefix = "事件 " + event.getEventNo();
            if (event.getPassReviewAt() == null || event.getPassConclusion() == null) {
                blockers.add(prefix + " 还没有合格复查结论");
                continue;
            }
            // 复查是否过期：合格结论必须在有效天数内，且下次复查日也不能已过期
            int validDays = event.getPassValidDays() == null
                    ? EquestrianDict.PASS_VALID_DAYS : event.getPassValidDays();
            LocalDate passDate = event.getPassReviewAt().toLocalDate();
            if (passDate.plusDays(validDays).isBefore(today)) {
                blockers.add(prefix + " 的合格复查已过期（" + validDays + " 天有效期，"
                        + passDate + " 出具），请重新复查");
            } else if (event.getNextReviewDate() != null
                    && event.getNextReviewDate().isBefore(today)) {
                blockers.add(prefix + " 合格复查设定的跟踪复查日（"
                        + event.getNextReviewDate() + "）已过，请重新复查确认");
            }
        }

        // 「结论基于旧事件」：最近一次合格复查之后若又登记了更新的事件，则该结论不能作为放行依据。
        // 这里新事件本身一定仍未关闭（在 openEvents 中）且无合格结论，上一条检查已经覆盖；
        // 额外再做一次显式判定，给出「存在更晚登记的新事件」的明确原因。
        if (!blockers.isEmpty()) {
            return blockers;
        }
        HealthEvent latest = eventRepository.findFirstByHorseIdOrderByIdDesc(horse.getId()).orElse(null);
        if (latest != null && openEvents.stream().noneMatch((e) -> e.getId().equals(latest.getId()))) {
            blockers.add("存在比合格复查更晚登记的新事件（" + latest.getEventNo() + "），当前结论基于旧事件链");
        }
        return blockers;
    }

    // ============================ 辅助 ============================

    /** 校验页面携带的事件链版本是否仍是最新；不是则抛 409 */
    private void assertChainCurrent(Long horseId, Long clientVersion, HealthEvent currentEvent) {
        if (clientVersion == null) {
            throw new ConflictException("缺少事件链版本信息，请重新载入最新事件链后再提交");
        }
        long latest = chainVersionOf(horseId);
        if (clientVersion.longValue() != latest) {
            throw new ConflictException("这匹马的健康事件链刚被其他人更新（有人登记了新伤病或完成了新的复查），"
                    + "你提交的结论基于旧状态，已被拒绝。请重新载入最新事件链后再操作");
        }
    }

    private long chainVersionOf(Long horseId) {
        return transitionRepository.findFirstByHorseIdOrderByIdDesc(horseId)
                .map(HealthTransition::getId)
                .orElse(0L);
    }

    private boolean isOverdue(HealthEvent event, LocalDate today) {
        return EquestrianDict.isHealthEventOpen(event.getStatus())
                && event.getNextReviewDate() != null
                && event.getNextReviewDate().isBefore(today);
    }

    private void requireReviewDate(LocalDate date, LocalDate today, String message) {
        if (date == null) {
            throw new BizException(message);
        }
        if (date.isBefore(today)) {
            throw new BizException("下次复查日不能早于今天");
        }
    }

    private String trimRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(field + "不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 500) {
            throw new BizException(field + "过长（最多 500 字）");
        }
        return trimmed;
    }

    private HealthEvent requireEvent(Long eventId) {
        if (eventId == null) {
            throw new BizException("请先选择健康事件");
        }
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new BizException("健康事件不存在，编号：" + eventId));
    }

    private Horse requireHorse(Long horseId) {
        if (horseId == null) {
            throw new BizException("请先选择马匹");
        }
        return horseRepository.findById(horseId)
                .orElseThrow(() -> new BizException("马匹不存在，编号：" + horseId));
    }

    private Horse lockHorse(Long horseId) {
        requireHorse(horseId);
        return horseRepository.findByIdForUpdate(horseId)
                .orElseThrow(() -> new BizException("马匹不存在，编号：" + horseId));
    }

    /** 事件编号：HE + 递增序号；按最大 id 估算，唯一约束兜底，冲突时顺延重试 */
    private String generateEventNo() {
        long sequence = eventRepository.count() + 1;
        String candidate;
        do {
            candidate = String.format("HE-%03d", sequence);
            sequence++;
        } while (eventRepository.findByEventNo(candidate).isPresent());
        return candidate;
    }

    /**
     * 追加一条流转历史。历史只追加不改写，chainVersion 记录的是「本条之后的链头流水号」，
     * 即插入后自身的主键（客户端用它判断页面是否过期）；调用方统一传 0 由这里落库后回填。
     */
    private HealthTransition appendTransition(HealthEvent event, String action, String fromStatus,
            String toStatus, String note, LocalDate nextReviewDate,
            String operatorName, String operatorRole, long chainVersion) {
        HealthTransition transition = new HealthTransition();
        transition.setEventId(event.getId());
        transition.setHorseId(event.getHorseId());
        transition.setAction(action);
        transition.setFromStatus(fromStatus);
        transition.setToStatus(toStatus);
        transition.setNote(note);
        transition.setNextReviewDate(nextReviewDate);
        transition.setOperatorName(operatorName);
        transition.setOperatorRole(operatorRole);
        transition.setChainVersion(chainVersion);
        transition.setCreatedAt(LocalDateTime.now());
        transition = transitionRepository.saveAndFlush(transition);
        if (transition.getChainVersion() == null || transition.getChainVersion() == 0L) {
            transition.setChainVersion(transition.getId());
            transitionRepository.save(transition);
        }
        return transition;
    }

    /**
     * 高风险事件登记：圈定该马今天及以后、未取消的排期，打标 + 落追溯表。
     * 已取消的、过去的排期不动；已在追溯表里的不重复落（uk_event_session 兜底）。
     */
    private List<LessonSession> markFutureSessionsAffected(HealthEvent event) {
        List<LessonSession> sessions = sessionRepository
                .findByHorseIdAndSessionDateGreaterThanEqualAndStatusNotOrderBySessionDateAscStartTimeAsc(
                        event.getHorseId(), LocalDate.now(), EquestrianDict.SESSION_CANCELED);
        List<LessonSession> marked = new ArrayList<>();
        for (LessonSession session : sessions) {
            session.setHealthAffected(true);
            sessionRepository.save(session);
            if (!eventSessionRepository.existsByEventIdAndSessionId(event.getId(), session.getId())) {
                HealthEventSession link = new HealthEventSession();
                link.setEventId(event.getId());
                link.setHorseId(event.getHorseId());
                link.setSessionId(session.getId());
                link.setCreatedAt(LocalDateTime.now());
                eventSessionRepository.save(link);
            }
            marked.add(session);
        }
        return marked;
    }

    /**
     * 放行后复位该马排期的受影响标记：只复位已经没有「未关闭事件」关联的场次，
     * 若期间又登记了新高风险事件（理论上放行事务内不会发生，防御性保留），其标记保留。
     */
    private void unmarkSessionsIfRecovered(Long horseId) {
        List<Long> sessionIds = eventSessionRepository.findSessionIdsByHorseId(horseId);
        if (sessionIds.isEmpty()) {
            return;
        }
        for (LessonSession session : sessionRepository.findByHorseIdAndIdIn(horseId, sessionIds)) {
            if (Boolean.TRUE.equals(session.getHealthAffected())
                    && !eventSessionRepository.isSessionStillAffected(session.getId())) {
                session.setHealthAffected(false);
                sessionRepository.save(session);
            }
        }
    }

    // ============================ 视图组装 ============================

    private HealthEventView toView(HealthEvent event, boolean includeDetails) {
        Horse horse = horseRepository.findById(event.getHorseId()).orElse(null);
        LocalDate today = LocalDate.now();

        boolean overdue = isOverdue(event, today);
        boolean passExpired = false;
        if (event.getPassReviewAt() != null && event.getPassValidDays() != null) {
            passExpired = event.getPassReviewAt().toLocalDate()
                    .plusDays(event.getPassValidDays()).isBefore(today);
        }

        List<HealthTransitionView> transitions = List.of();
        List<AffectedSessionView> affected = List.of();
        if (includeDetails) {
            transitions = transitionRepository.findByEventIdOrderByIdAsc(event.getId())
                    .stream().map(this::toTransitionView).toList();
            affected = eventSessionRepository.findByEventIdOrderByIdAsc(event.getId())
                    .stream().map(this::toAffectedView).toList();
        }

        Long latestEventId = eventRepository.findFirstByHorseIdOrderByIdDesc(event.getHorseId())
                .map(HealthEvent::getId).orElse(null);

        return new HealthEventView(
                event.getId(),
                event.getEventNo(),
                event.getHorseId(),
                horse == null ? "" : horse.getHorseNo(),
                horse == null ? "马匹已删除" : horse.getName(),
                horse == null ? null : horse.getStatus(),
                horse == null ? "" : EquestrianDict.horseStatusName(horse.getStatus()),
                event.getOccurredAt(),
                event.getSeverity(),
                EquestrianDict.severityName(event.getSeverity()),
                event.getSymptom(),
                event.getTreatmentAdvice(),
                event.getStatus(),
                EquestrianDict.healthEventStatusName(event.getStatus()),
                event.getNextReviewDate(),
                event.getPassConclusion(),
                event.getPassReviewAt(),
                event.getPassValidDays(),
                passExpired,
                overdue,
                event.getClosedAt(),
                event.getVersion(),
                chainVersionOf(event.getHorseId()),
                latestEventId,
                event.getCreatedAt(),
                transitions,
                affected);
    }

    private HealthTransitionView toTransitionView(HealthTransition transition) {
        return new HealthTransitionView(
                transition.getId(),
                transition.getEventId(),
                transition.getHorseId(),
                transition.getAction(),
                EquestrianDict.healthActionName(transition.getAction()),
                transition.getFromStatus(),
                EquestrianDict.healthEventStatusName(transition.getFromStatus()),
                transition.getToStatus(),
                EquestrianDict.healthEventStatusName(transition.getToStatus()),
                transition.getNote(),
                transition.getNextReviewDate(),
                transition.getOperatorName(),
                transition.getOperatorRole(),
                EquestrianDict.operatorRoleName(transition.getOperatorRole()),
                transition.getChainVersion(),
                transition.getCreatedAt());
    }

    private AffectedSessionView toAffectedView(HealthEventSession link) {
        LessonSession session = sessionRepository.findById(link.getSessionId()).orElse(null);
        if (session == null) {
            return new AffectedSessionView(link.getId(), link.getEventId(), link.getSessionId(),
                    "排期已删除", "", null, "", "", "", "", 0, 0, false, link.getCreatedAt());
        }
        Lesson lesson = lessonRepository.findById(session.getLessonId()).orElse(null);
        Coach coach = coachRepository.findById(session.getCoachId()).orElse(null);
        return new AffectedSessionView(
                link.getId(),
                link.getEventId(),
                session.getId(),
                lesson == null ? "课程已删除" : lesson.getName(),
                coach == null ? "教练已删除" : coach.getName(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                EquestrianDict.sessionStatusName(session.getStatus()),
                session.getBookedCount(),
                session.getCapacity(),
                eventSessionRepository.isSessionStillAffected(session.getId()),
                link.getCreatedAt());
    }
}

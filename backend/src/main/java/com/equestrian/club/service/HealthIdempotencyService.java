package com.equestrian.club.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.equestrian.club.common.BizException;
import com.equestrian.club.dict.EquestrianDict;
import com.equestrian.club.domain.HealthOpLedger;
import com.equestrian.club.domain.HealthOpLedgerRepository;

/**
 * 健康处置幂等控制。
 *
 * <p>同一 request_key 的登记 / 流转 / 放行，无论用户点多少次、网络重试多少次，
 * 业务动作只执行一次：第一次占键成功后执行业务并回填目标；后续请求识别为重放，
 * 由调用方回查并返回首次结果。
 *
 * <p>占键与业务放在同一个数据库事务里：业务校验失败回滚时占用一并回滚，用户可直接重试；
 * 两个并发重复请求靠 request_key 唯一约束，必有一个落键失败转成「重放」分支。
 */
@Service
public class HealthIdempotencyService {

    public static final String SCOPE_EVENT = "EVENT";
    public static final String SCOPE_CLEAR = "CLEAR";

    private final HealthOpLedgerRepository ledgerRepository;

    public HealthIdempotencyService(HealthOpLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * 尝试占用一个幂等键。
     *
     * @return 为空表示抢占成功，可以执行业务；非空表示该键已被占用（重放），
     *         调用方应据此回查首次结果返回
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<HealthOpLedger> acquire(String requestKey, String scope, Long horseId, String operatorName) {
        validate(requestKey, scope, horseId, operatorName);
        Optional<HealthOpLedger> exist = ledgerRepository.findByRequestKey(requestKey);
        if (exist.isPresent()) {
            return exist;
        }
        HealthOpLedger ledger = new HealthOpLedger();
        ledger.setRequestKey(requestKey);
        ledger.setScope(scope);
        ledger.setHorseId(horseId);
        ledger.setOperatorName(operatorName);
        ledger.setCreatedAt(LocalDateTime.now());
        try {
            ledgerRepository.saveAndFlush(ledger);
        } catch (DataIntegrityViolationException e) {
            // 并发的另一个相同请求刚抢先落键：按重放处理
            return ledgerRepository.findByRequestKey(requestKey);
        }
        return Optional.empty();
    }

    /** 业务成功后把首次结果目标绑定到幂等键上（事件类操作填事件 id；放行保持 null） */
    @Transactional(propagation = Propagation.REQUIRED)
    public void bindEvent(String requestKey, Long eventId) {
        ledgerRepository.findByRequestKey(requestKey).ifPresent((ledger) -> ledger.setEventId(eventId));
    }

    private void validate(String requestKey, String requestScope, Long horseId, String operatorName) {
        if (requestKey == null || requestKey.isBlank()) {
            throw new BizException("缺少操作幂等键（requestKey），请刷新页面后重试，以防重复提交");
        }
        if (requestKey.length() > 64) {
            throw new BizException("操作幂等键长度不合法");
        }
        if (!SCOPE_EVENT.equals(requestScope) && !SCOPE_CLEAR.equals(requestScope)) {
            throw new BizException("操作类型不合法");
        }
        if (horseId == null) {
            throw new BizException("请先选择马匹");
        }
        if (operatorName == null || operatorName.isBlank()) {
            throw new BizException("请填写操作人姓名");
        }
        if (operatorName.trim().length() > 64) {
            throw new BizException("操作人姓名过长");
        }
    }

    /** 校验操作人角色取值合法并返回（取值本身不合法直接拒绝） */
    public String requireValidRole(String role) {
        if (!EquestrianDict.isValidOperatorRole(role)) {
            throw new BizException("操作人角色不合法，只支持 STAFF 普通工作人员 / MANAGER 负责人");
        }
        return role;
    }
}

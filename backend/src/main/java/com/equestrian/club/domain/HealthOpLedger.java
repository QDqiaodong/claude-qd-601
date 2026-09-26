package com.equestrian.club.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 健康操作幂等台账。
 *
 * <p>登记事件、事件流转、马匹级复训放行共用。前端为同一次提交生成一个 request_key，
 * 用户重复点击 / 网络重试时该键不变；request_key 在本表唯一，命中已有行即视为重放，
 * 直接回查并返回首次结果，不会产生第二条历史或第二次改变马匹状态。
 */
@Entity
@Table(name = "health_op_ledger")
public class HealthOpLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_key", nullable = false, unique = true, length = 64)
    private String requestKey;

    /** EVENT 针对单个事件 / CLEAR 马匹级复训放行 */
    @Column(name = "scope", nullable = false, length = 16)
    private String scope;

    /** scope=EVENT 时首次操作落在哪条事件上（放行时为空） */
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(name = "operator_name", nullable = false, length = 64)
    private String operatorName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

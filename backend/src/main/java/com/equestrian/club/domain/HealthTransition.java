package com.equestrian.club.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 健康事件流转历史（append-only）。
 *
 * <p>登记、补充处置、开始观察、申请复查、复查后继续观察、复查合格、复训放行关闭，
 * 每发生一次就插入一行；已经发生的处置历史不提供任何更新/删除入口，
 * 因此后续操作只能往后追加，不可能覆盖更早的记录。
 *
 * <p>{@code chainVersion} 记录提交者当时看到的「马匹事件链版本」（该马历史最大流水号），
 * 提交时与最新值比对，不一致说明期间有别人登记了新伤病或完成了新的复查，直接 409。
 */
@Entity
@Table(name = "horse_health_transition")
public class HealthTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    /** REGISTER / PROCESS / START_OBSERVE / REQUEST_REVIEW / REVIEW_CONTINUE / REVIEW_PASS / CLOSE */
    @Column(name = "action", nullable = false, length = 24)
    private String action;

    @Column(name = "from_status", length = 24)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 24)
    private String toStatus;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    @Column(name = "operator_name", nullable = false, length = 64)
    private String operatorName;

    /** STAFF 普通工作人员 / MANAGER 负责人 */
    @Column(name = "operator_role", nullable = false, length = 16)
    private String operatorRole;

    @Column(name = "chain_version", nullable = false)
    private Long chainVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public void setOperatorRole(String operatorRole) {
        this.operatorRole = operatorRole;
    }

    public Long getChainVersion() {
        return chainVersion;
    }

    public void setChainVersion(Long chainVersion) {
        this.chainVersion = chainVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

package com.equestrian.club.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 马匹健康事件：「伤病发生 -> 处置 -> 观察 -> 复查 -> 复训放行关闭」闭环的主体。
 *
 * <p>状态机：PENDING 待处理 / OBSERVING 观察中 / REVIEW_PENDING 待复查 / CLOSED 已关闭。
 * 每次状态变化与补充处置都会向 horse_health_transition 追加一条历史，历史只追加不改写；
 * 本实体只保存当前快照（当前状态、下次复查日、最近一次合格复查结论）。
 *
 * <p>{@code version} 是 JPA 乐观锁版本：两人同时处理同一事件时，后提交的一方会因版本过期被拒绝。
 */
@Entity
@Table(name = "horse_health_event")
public class HealthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_no", nullable = false, unique = true, length = 32)
    private String eventNo;

    @Column(name = "horse_id", nullable = false)
    private Long horseId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** LOW 低 / MEDIUM 中 / HIGH 高（高风险登记时马匹立即休养） */
    @Column(name = "severity", nullable = false, length = 16)
    private String severity;

    @Column(name = "symptom", nullable = false, length = 500)
    private String symptom;

    @Column(name = "treatment_advice", nullable = false, length = 500)
    private String treatmentAdvice;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    /** 最近一次合格复查结论（REVIEW_PASS 写入），放行时校验在有效期内 */
    @Column(name = "pass_conclusion", length = 500)
    private String passConclusion;

    @Column(name = "pass_review_at")
    private LocalDateTime passReviewAt;

    @Column(name = "pass_valid_days")
    private Integer passValidDays;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** 乐观锁版本：并发提交旧结论时后到的一方会被拒绝 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventNo() {
        return eventNo;
    }

    public void setEventNo(String eventNo) {
        this.eventNo = eventNo;
    }

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSymptom() {
        return symptom;
    }

    public void setSymptom(String symptom) {
        this.symptom = symptom;
    }

    public String getTreatmentAdvice() {
        return treatmentAdvice;
    }

    public void setTreatmentAdvice(String treatmentAdvice) {
        this.treatmentAdvice = treatmentAdvice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public String getPassConclusion() {
        return passConclusion;
    }

    public void setPassConclusion(String passConclusion) {
        this.passConclusion = passConclusion;
    }

    public LocalDateTime getPassReviewAt() {
        return passReviewAt;
    }

    public void setPassReviewAt(LocalDateTime passReviewAt) {
        this.passReviewAt = passReviewAt;
    }

    public Integer getPassValidDays() {
        return passValidDays;
    }

    public void setPassValidDays(Integer passValidDays) {
        this.passValidDays = passValidDays;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

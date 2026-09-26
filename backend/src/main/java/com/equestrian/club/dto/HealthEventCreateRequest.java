package com.equestrian.club.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 登记健康事件请求体。
 */
public class HealthEventCreateRequest extends OperatorRequest {

    private Long horseId;

    /** 发生时间，可空：不填默认当前时间 */
    private LocalDateTime occurredAt;

    /** LOW / MEDIUM / HIGH，必填 */
    private String severity;

    /** 症状说明，必填 */
    private String symptom;

    /** 处置建议，必填 */
    private String treatmentAdvice;

    /** 预计复查日，必填（不能早于登记当天） */
    private LocalDate nextReviewDate;

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

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }
}

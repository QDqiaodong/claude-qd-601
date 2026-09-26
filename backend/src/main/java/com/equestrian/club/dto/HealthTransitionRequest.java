package com.equestrian.club.dto;

import java.time.LocalDate;

/**
 * 健康事件流转请求体。
 *
 * <p>动作：PROCESS 补充处置 / START_OBSERVE 开始观察 / REQUEST_REVIEW 申请复查 /
 * REVIEW_CONTINUE 复查后继续观察 / REVIEW_PASS 复查合格。
 * chainVersion 是打开页面时看到的马匹事件链版本，提交时用于并发冲突校验。
 */
public class HealthTransitionRequest extends OperatorRequest {

    private String action;

    /** 本次处置 / 复查说明 */
    private String note;

    /** 本次动作设定（或调整）的下次复查日；观察、复查类动作必填 */
    private LocalDate nextReviewDate;

    /** REVIEW_PASS 时的合格复查结论，必填 */
    private String conclusion;

    /** 提交者基于的马匹事件链版本（最新一条流转流水号），必填 */
    private Long chainVersion;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public Long getChainVersion() {
        return chainVersion;
    }

    public void setChainVersion(Long chainVersion) {
        this.chainVersion = chainVersion;
    }
}

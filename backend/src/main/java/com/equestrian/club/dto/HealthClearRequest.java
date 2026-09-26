package com.equestrian.club.dto;

/**
 * 马匹级复训放行请求体（负责人专属）。
 *
 * <p>放行是马匹级动作：会把该马所有「复查合格且在有效期内」的未关闭事件统一关闭，
 * 并把马匹从休养恢复为在役。chainVersion 仍做并发保护——
 * 打开放行弹窗后若又有人登记新伤病 / 完成新复查，旧提交必须被拒绝。
 */
public class HealthClearRequest extends OperatorRequest {

    private Long horseId;

    /** 放行说明（负责人意见），必填 */
    private String note;

    /** 提交者基于的马匹事件链版本，必填 */
    private Long chainVersion;

    public Long getHorseId() {
        return horseId;
    }

    public void setHorseId(Long horseId) {
        this.horseId = horseId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getChainVersion() {
        return chainVersion;
    }

    public void setChainVersion(Long chainVersion) {
        this.chainVersion = chainVersion;
    }
}

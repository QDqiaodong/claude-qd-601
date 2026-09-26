package com.equestrian.club.common;

/**
 * 并发冲突异常：提交所基于的事件版本 / 马匹事件链版本已经过期
 * （别人刚登记了新伤病或完成了新的复查），本次提交被拒绝，需要重新载入最新事件链。
 * 统一异常处理会转成 409。
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

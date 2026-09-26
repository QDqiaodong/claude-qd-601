package com.equestrian.club.common;

/**
 * 权限异常：当前操作人角色无权执行该动作（如普通工作人员尝试复训放行）。
 * 统一异常处理会转成 403。业务校验必须在后端完成，不能只靠前端隐藏按钮。
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

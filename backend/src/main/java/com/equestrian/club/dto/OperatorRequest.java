package com.equestrian.club.dto;

/**
 * 健康处置类请求的公共字段：操作人姓名、角色与幂等键。
 * 系统没有登录态，页面顶部选择「当前身份」；角色由后端再次校验，
 * 普通工作人员即使绕过前端直接调接口也无法完成负责人专属的复训放行。
 */
public class OperatorRequest {

    /** 操作人姓名，必填 */
    private String operatorName;

    /** STAFF 普通工作人员 / MANAGER 负责人，必填 */
    private String operatorRole;

    /** 幂等键：同一次提交重复点击保持不变；为空时由后端拒绝，强制前端生成 */
    private String requestKey;

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

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }
}

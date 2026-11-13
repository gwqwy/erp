package cn.tiangong.module.bpm.api;

/**
 * 审批驳回事件：业务模块监听后把单据退回草稿。
 */
public record ApprovalRejectedEvent(String bizType, String chatterBizType, Long bizId, String bizNo) {
}

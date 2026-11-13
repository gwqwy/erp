package cn.tiangong.module.bpm.api;

/**
 * 审批全部通过事件：业务模块监听后执行各自的 audit（库存/财务过账等）。
 */
public record ApprovalCompletedEvent(String bizType, String chatterBizType, Long bizId, String bizNo) {
}

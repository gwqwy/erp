package cn.tiangong.framework.biz.common.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务单据统一状态（《02-总体架构》§4.1）：
 * 草稿 10 → 待审批 20（M2 接 Flowable）→ 已审核 30 → 已完成 40；作废 50。
 * 只有已审核状态的单据触发库存/财务过账。
 */
@Getter
@AllArgsConstructor
public enum BizOrderStatus {

    DRAFT(10, "草稿"),
    APPROVING(20, "待审批"),
    AUDITED(30, "已审核"),
    DONE(40, "已完成"),
    VOIDED(50, "已作废");

    private final Integer value;
    private final String label;

    public static BizOrderStatus of(Integer value) {
        for (BizOrderStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}

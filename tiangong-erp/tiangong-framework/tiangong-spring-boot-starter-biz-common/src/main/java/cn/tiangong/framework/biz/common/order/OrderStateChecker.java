package cn.tiangong.framework.biz.common.order;

import cn.tiangong.framework.web.core.exception.ServiceException;

import java.util.Set;

/**
 * 单据状态流转校验器：所有单据 service 的状态变更必须经过这里，禁止裸写状态号。
 */
public class OrderStateChecker {

    private OrderStateChecker() {
    }

    /** 校验当前状态属于期望集合，否则抛业务异常 */
    public static void require(Integer currentStatus, String orderNo, BizOrderStatus... expected) {
        BizOrderStatus current = BizOrderStatus.of(currentStatus);
        for (BizOrderStatus status : expected) {
            if (status == current) {
                return;
            }
        }
        StringBuilder expectedMsg = new StringBuilder();
        for (BizOrderStatus status : expected) {
            expectedMsg.append(status.getLabel()).append("、");
        }
        expectedMsg.deleteCharAt(expectedMsg.length() - 1);
        throw new ServiceException(4_00_0001, String.format("单据 %s 当前状态为「%s」，仅「%s」可执行该操作",
                orderNo, current == null ? "未知" : current.getLabel(), expectedMsg));
    }

    /** 判断状态是否在集合内 */
    public static boolean in(Integer currentStatus, Set<BizOrderStatus> statuses) {
        BizOrderStatus current = BizOrderStatus.of(currentStatus);
        return current != null && statuses.contains(current);
    }
}

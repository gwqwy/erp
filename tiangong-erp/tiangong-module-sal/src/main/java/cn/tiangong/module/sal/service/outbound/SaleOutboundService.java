package cn.tiangong.module.sal.service.outbound;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOutboundDO;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOutboundItemDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SaleOutboundService {

    PageResult<ErpSaleOutboundDO> getOutboundPage(PageParam pageParam, String no, Long customerId,
                                                  Long orderId, Integer status);

    OutboundDetail getOutboundDetail(Long id);

    Long createOutbound(Long customerId, Long warehouseId, Long orderId, LocalDate outboundDate,
                        String remark, List<Item> items);

    void deleteOutbound(Long id);

    /** 审核：草稿 → 已审核，库存过账（出库，防超卖）+ 回写订单执行进度 */
    void auditOutbound(Long id);

    /** 提交审批：草稿 → 待审批 */
    void submitApproval(Long id);

    /** 审批驳回回调：待审批 → 草稿 */
    void onApprovalRejected(Long id);

    void voidOutbound(Long id);

    /** 红字冲销：已审核且应收未核销——货物回库、应收作废、红字凭证、回退订单进度 */
    void reverseOutbound(Long id);

    record Item(Long orderItemId, Long productId, Long unitId, BigDecimal qty,
                BigDecimal price, String remark) {
    }

    record OutboundDetail(ErpSaleOutboundDO outbound, List<ErpSaleOutboundItemDO> items) {
    }
}

package cn.tiangong.module.pur.service.inbound;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseInboundDO;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseInboundItemDO;
import cn.tiangong.module.pur.service.order.PurchaseOrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PurchaseInboundService {

    PageResult<ErpPurchaseInboundDO> getInboundPage(PageParam pageParam, String no, Long supplierId,
                                                    Long orderId, Integer status);

    InboundDetail getInboundDetail(Long id);

    Long createInbound(Long supplierId, Long warehouseId, Long orderId, LocalDate inboundDate,
                       String remark, List<Item> items);

    void deleteInbound(Long id);

    /** 审核：草稿 → 已审核，同时库存过账 + 回写采购订单执行进度 */
    void auditInbound(Long id);

    /** 提交审批：草稿 → 待审批 */
    void submitApproval(Long id);

    /** 审批驳回回调：待审批 → 草稿 */
    void onApprovalRejected(Long id);

    void voidInbound(Long id);

    /** 红字冲销：已审核且应付未核销时可冲销——库存反向出账、应付作废、生成红字凭证、回退订单进度 */
    void reverseInbound(Long id);

    record Item(Long orderItemId, Long productId, Long unitId, BigDecimal qty,
                BigDecimal price, String remark) {
    }

    record InboundDetail(ErpPurchaseInboundDO inbound, List<ErpPurchaseInboundItemDO> items) {
    }
}

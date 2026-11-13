package cn.tiangong.module.pur.service.order;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderDO;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderItemDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PurchaseOrderService {

    PageResult<ErpPurchaseOrderDO> getOrderPage(PageParam pageParam, String no, Long supplierId, Integer status);

    /** 订单 + 明细（编辑/详情用） */
    OrderDetail getOrderDetail(Long id);

    /** 订单明细（供入库单引用，含剩余可入库数量） */
    List<ErpPurchaseOrderItemDO> getItemList(Long orderId);

    Long createOrder(Long supplierId, LocalDate orderDate, LocalDate expectDate,
                     String remark, List<Item> items);

    void updateOrder(Long id, Long supplierId, LocalDate orderDate, LocalDate expectDate,
                     String remark, List<Item> items);

    void deleteOrder(Long id);

    /** 审核：草稿 → 已审核（或审批通过后自动执行） */
    void auditOrder(Long id);

    /** 提交审批：草稿 → 待审批（未配置审批流时报错，应直接审核） */
    void submitApproval(Long id);

    /** 审批驳回回调：待审批 → 草稿 */
    void onApprovalRejected(Long id);

    /** 作废：仅草稿可作废（已审核走红字冲销，M2） */
    void voidOrder(Long id);

    record Item(Long productId, Long unitId, BigDecimal qty, BigDecimal price,
                BigDecimal taxRate, String remark) {
    }

    record OrderDetail(ErpPurchaseOrderDO order, List<ErpPurchaseOrderItemDO> items) {
    }
}

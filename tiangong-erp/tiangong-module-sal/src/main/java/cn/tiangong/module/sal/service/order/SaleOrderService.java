package cn.tiangong.module.sal.service.order;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOrderDO;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOrderItemDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SaleOrderService {

    PageResult<ErpSaleOrderDO> getOrderPage(PageParam pageParam, String no, Long customerId, Integer status);

    OrderDetail getOrderDetail(Long id);

    List<ErpSaleOrderItemDO> getItemList(Long orderId);

    Long createOrder(Long customerId, LocalDate orderDate, LocalDate expectDate,
                     String remark, List<Item> items);

    void updateOrder(Long id, Long customerId, LocalDate orderDate, LocalDate expectDate,
                     String remark, List<Item> items);

    void deleteOrder(Long id);

    void auditOrder(Long id);

    /** 提交审批：草稿 → 待审批 */
    void submitApproval(Long id);

    /** 审批驳回回调：待审批 → 草稿 */
    void onApprovalRejected(Long id);

    void voidOrder(Long id);

    /** 更新累计订金 */
    void updateDeposit(Long id, java.math.BigDecimal depositAmount);

    record Item(Long productId, Long unitId, BigDecimal qty, BigDecimal price,
                BigDecimal taxRate, String remark) {
    }

    record OrderDetail(ErpSaleOrderDO order, List<ErpSaleOrderItemDO> items) {
    }
}

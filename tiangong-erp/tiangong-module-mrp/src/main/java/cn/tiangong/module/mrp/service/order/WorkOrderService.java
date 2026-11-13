package cn.tiangong.module.mrp.service.order;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpWorkOrderDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpWorkOrderItemDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface WorkOrderService {

    PageResult<ErpMrpWorkOrderDO> getWorkOrderPage(PageParam pageParam, String no, Long productId, Integer status);

    WorkOrderDetail getWorkOrderDetail(Long id);

    /** 工单材料需求行（供领料单引用，含剩余可领数量） */
    List<ErpMrpWorkOrderItemDO> getItemList(Long workOrderId);

    Long createWorkOrder(Long productId, Long bomId, Long warehouseId, BigDecimal qty,
                         LocalDate planStartDate, LocalDate planFinishDate, String remark);

    void deleteWorkOrder(Long id);

    /** 审核（下达）：按 BOM 展开生成材料需求行 */
    void auditWorkOrder(Long id);

    void voidWorkOrder(Long id);

    /** 完工入库：成品过账（produce_in，成本按累计领料成本估算），全部完工自动关闭 */
    BigDecimal finishWorkOrder(Long id, BigDecimal qty, Long warehouseId);

    record WorkOrderDetail(ErpMrpWorkOrderDO order, List<ErpMrpWorkOrderItemDO> items) {
    }
}

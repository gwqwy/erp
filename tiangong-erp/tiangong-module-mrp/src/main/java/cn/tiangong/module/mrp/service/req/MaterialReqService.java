package cn.tiangong.module.mrp.service.req;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpMaterialReqDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpMaterialReqItemDO;

import java.math.BigDecimal;
import java.util.List;

public interface MaterialReqService {

    PageResult<ErpMrpMaterialReqDO> getReqPage(PageParam pageParam, String no, Long workOrderId, Integer status);

    ReqDetail getReqDetail(Long id);

    Long createReq(Long workOrderId, Long warehouseId, String remark, List<Item> items);

    void deleteReq(Long id);

    /** 审核：库存出账（material_out）+ 回写工单材料行 issued_qty + 工单转生产中 */
    void auditReq(Long id);

    void voidReq(Long id);

    record Item(Long workOrderItemId, Long productId, BigDecimal qty, String remark) {
    }

    record ReqDetail(ErpMrpMaterialReqDO req, List<ErpMrpMaterialReqItemDO> items) {
    }
}

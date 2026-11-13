package cn.tiangong.module.mrp.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 领料明细：work_order_item_id 对应工单材料需求行，审核回写 issued_qty。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_mrp_material_req_item")
public class ErpMrpMaterialReqItemDO extends BaseDO {

    private Long reqId;

    private Long workOrderItemId;

    private Long productId;

    private BigDecimal qty;

    private String remark;
}

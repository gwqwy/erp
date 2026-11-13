package cn.tiangong.module.mrp.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工单材料需求行：下达时按 BOM 展开到叶子生成；issued_qty 由领料单审核回写。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_mrp_work_order_item")
public class ErpMrpWorkOrderItemDO extends BaseDO {

    private Long workOrderId;

    private Long productId;

    private Long unitId;

    /** 需求数量（含损耗） */
    private BigDecimal qty;

    /** 已领数量 */
    private BigDecimal issuedQty;

    private String remark;
}

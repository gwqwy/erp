package cn.tiangong.module.mrp.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产领料单：引用工单，审核后经库存引擎出账（material_out），成本取当时加权平均。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_mrp_material_req")
public class ErpMrpMaterialReqDO extends BaseDO {

    private String no;

    private Long workOrderId;

    private Long warehouseId;

    private BigDecimal totalQty;

    private String remark;

    /** 10 草稿 30 已审核 50 已作废 */
    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}

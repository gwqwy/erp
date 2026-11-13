package cn.tiangong.module.mrp.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产工单：审核（下达）时按 BOM 展开生成材料需求行；领料过账走库存引擎（material_out）；
 * 完工入库走库存引擎（produce_in），成本按累计领料成本估算。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_mrp_work_order")
public class ErpMrpWorkOrderDO extends BaseDO {

    private String no;

    private Long productId;

    private Long bomId;

    private Long warehouseId;

    /** 计划生产数量 */
    private BigDecimal qty;

    /** 累计完工入库数量 */
    private BigDecimal finishedQty;

    private LocalDate planStartDate;

    private LocalDate planFinishDate;

    private String remark;

    /** 10 计划(草稿) 30 已下达 40 生产中 50 已完成 60 已作废 */
    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}

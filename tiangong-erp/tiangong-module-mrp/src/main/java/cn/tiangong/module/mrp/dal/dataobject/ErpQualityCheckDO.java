package cn.tiangong.module.mrp.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 质检记录（记录型：来料检 IQC / 完工检 FQC，不阻塞库存流程）。
 * check_type：iqc 来料检（关联采购入库单）/ fqc 完工检（关联生产工单）。
 * result：1 合格 / 2 不合格 / 3 让步接收。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_quality_check")
public class ErpQualityCheckDO extends BaseDO {

    private String no;

    private String checkType;

    private Long sourceId;

    private String sourceNo;

    private Long productId;

    private BigDecimal checkQty;

    private BigDecimal qualifiedQty;

    private String inspector;

    private LocalDate checkDate;

    private Integer result;

    private String remark;
}

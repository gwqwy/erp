package cn.tiangong.module.mrp.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * BOM 组件行：qty 为相对 BOM 基数的单耗；loss_rate 为损耗率 %。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_mrp_bom_item")
public class ErpMrpBomItemDO extends BaseDO {

    private Long bomId;

    private Long productId;

    private BigDecimal qty;

    /** 损耗率 %（10 = 10%） */
    private BigDecimal lossRate;

    private String remark;
}

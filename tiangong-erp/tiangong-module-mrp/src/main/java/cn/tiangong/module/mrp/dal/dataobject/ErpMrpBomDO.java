package cn.tiangong.module.mrp.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * BOM（Metasfast 多版本思想）：每个产品可有多个版本，is_default=1 的版本用于工单展开与 MRP。
 * qty = 产出的成品基数（组件单耗相对该基数）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_mrp_bom")
public class ErpMrpBomDO extends BaseDO {

    private String no;

    /** 成品/半成品 */
    private Long productId;

    private Integer version;

    /** 产出数量基数 */
    private BigDecimal qty;

    /** 0 草稿 1 启用 */
    private Integer status;

    private Integer isDefault;

    private String remark;
}

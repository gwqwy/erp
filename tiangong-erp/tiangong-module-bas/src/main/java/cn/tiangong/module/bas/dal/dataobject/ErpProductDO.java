package cn.tiangong.module.bas.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 产品（借鉴 Odoo product.template 的多单位与价格字段）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_product")
public class ErpProductDO extends BaseDO {

    private String no;

    private String name;

    private Long categoryId;

    private Long unitId;

    private String spec;

    private String barcode;

    private BigDecimal purchasePrice;

    private BigDecimal salePrice;

    private BigDecimal taxRate;

    private BigDecimal stockMin;

    private BigDecimal stockMax;

    private Boolean isBatch;

    private Boolean isSerial;

    private Integer status;

    private String remark;
}

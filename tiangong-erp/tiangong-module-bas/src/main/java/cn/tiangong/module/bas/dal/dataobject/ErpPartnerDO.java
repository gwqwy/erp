package cn.tiangong.module.bas.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 往来单位：客户与供应商合一（借鉴 Odoo res.partner / Metasfresh C_BPartner 思想）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_partner")
public class ErpPartnerDO extends BaseDO {

    private String no;

    private String name;

    private String shortName;

    private Boolean isCustomer;

    private Boolean isSupplier;

    private String taxNo;

    private String address;

    private String contact;

    private String phone;

    private BigDecimal creditLimit;

    private Integer status;

    private String remark;
}

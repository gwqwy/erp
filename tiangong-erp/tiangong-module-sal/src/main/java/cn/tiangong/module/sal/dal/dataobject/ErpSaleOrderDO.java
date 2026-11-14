package cn.tiangong.module.sal.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单。price 口径：含税单价。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_sale_order")
public class ErpSaleOrderDO extends BaseDO {

    private String no;

    private Long customerId;

    private LocalDate orderDate;

    private LocalDate expectDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private BigDecimal totalTax;

    private BigDecimal totalAmountTax;

    private String remark;

    private java.math.BigDecimal depositAmount;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}

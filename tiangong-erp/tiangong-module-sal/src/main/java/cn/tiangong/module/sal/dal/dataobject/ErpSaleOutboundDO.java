package cn.tiangong.module.sal.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售出库单：orderId 为空表示无来源直接出库；审核时经库存引擎过账（direction=-1，防超卖）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_sale_outbound")
public class ErpSaleOutboundDO extends BaseDO {

    private String no;

    private Long customerId;

    private Long warehouseId;

    private Long orderId;

    private LocalDate outboundDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}

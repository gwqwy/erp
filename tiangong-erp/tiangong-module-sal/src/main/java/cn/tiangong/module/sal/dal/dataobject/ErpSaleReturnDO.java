package cn.tiangong.module.sal.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售退货单：审核后货物回库（走库存引擎），凭证经事件生成。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_sale_return")
public class ErpSaleReturnDO extends BaseDO {

    private String no;

    private Long customerId;

    private Long warehouseId;

    private Long orderId;

    private LocalDate returnDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
}

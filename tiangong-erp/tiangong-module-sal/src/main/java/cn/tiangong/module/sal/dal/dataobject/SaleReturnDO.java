package cn.tiangong.module.sal.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_sale_return")
public class SaleReturnDO extends BaseDO {

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

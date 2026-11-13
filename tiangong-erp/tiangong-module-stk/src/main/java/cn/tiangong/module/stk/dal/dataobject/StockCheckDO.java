package cn.tiangong.module.stk.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_stock_check")
public class StockCheckDO extends BaseDO {

    private String no;

    private Long warehouseId;

    private LocalDate checkDate;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;

}

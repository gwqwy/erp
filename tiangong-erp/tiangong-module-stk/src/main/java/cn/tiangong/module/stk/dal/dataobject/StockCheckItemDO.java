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
@TableName("erp_stock_check_item")
public class StockCheckItemDO extends BaseDO {

    private Long checkId;

    private Long productId;

    private BigDecimal bookQty;

    private BigDecimal realQty;

}

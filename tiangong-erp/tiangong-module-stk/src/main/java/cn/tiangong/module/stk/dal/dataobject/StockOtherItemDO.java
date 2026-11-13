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
@TableName("erp_stock_other_item")
public class StockOtherItemDO extends BaseDO {

    private Long otherId;

    private Long productId;

    private BigDecimal qty;

    private BigDecimal price;

    private String remark;

}

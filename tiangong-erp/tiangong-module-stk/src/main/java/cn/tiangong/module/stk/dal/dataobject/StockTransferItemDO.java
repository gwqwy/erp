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
@TableName("erp_stock_transfer_item")
public class StockTransferItemDO extends BaseDO {

    private Long transferId;

    private Long productId;

    private BigDecimal qty;

    private String remark;

}

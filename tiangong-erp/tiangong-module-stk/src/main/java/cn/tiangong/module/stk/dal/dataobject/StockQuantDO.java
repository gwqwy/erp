package cn.tiangong.module.stk.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 即时库存账（借鉴 Odoo stock.quant）：维度为 公司×仓库×库位×产品×批次 的当前结存。
 * 账表不参与逻辑删除与审计填充，更新必须带乐观锁 version。
 */
@Data
@TableName("erp_stock_quant")
public class StockQuantDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long companyId;

    private Long warehouseId;

    /** 库位，M1 默认 0 */
    private Long locationId;

    private Long productId;

    /** 无批次为空串，保证唯一维度完整 */
    private String batchNo;

    private BigDecimal qty;

    private BigDecimal lockedQty;

    /** 移动加权平均成本 */
    private BigDecimal avgCost;

    @Version
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

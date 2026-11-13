package cn.tiangong.module.bas.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_partner_price")
public class ErpPartnerPriceDO extends BaseDO {

    private Long partnerId;

    private Long productId;

    private BigDecimal price;

    private String remark;
}

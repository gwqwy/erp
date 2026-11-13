package cn.tiangong.module.crm.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 商机（销售漏斗）。stage：1 初步接触 / 2 需求确认 / 3 方案报价 / 4 商务谈判 / 5 赢单 / 6 输单。
 * 客户复用 erp_partner（is_customer）。赢单后可参照创建销售订单（M5）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_business")
public class CrmBusinessDO extends BaseDO {

    private String name;

    private Long partnerId;

    private BigDecimal amount;

    /** 漏斗阶段 1~6 */
    private Integer stage;

    private LocalDate expectDate;

    /** 来源线索，可空 */
    private Long sourceLeadId;

    private Long ownerUserId;

    private String remark;
}

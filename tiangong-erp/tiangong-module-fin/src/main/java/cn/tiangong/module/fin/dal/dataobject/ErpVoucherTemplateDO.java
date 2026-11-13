package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 凭证模板（Metasfresh 业务财务联动思想）：业务类型 × 事件 → 分录规则，页面可配置。
 * 例：sale_out@audit → 借 1122 应收账款 / 贷 6001 主营业务收入。
 * M2 金额来源固定为单据总额（amountSource=TOTAL），表达式引擎 M3 扩展。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_voucher_template")
public class ErpVoucherTemplateDO extends BaseDO {

    /** 业务类型：sale_out / purchase_in / receipt / payment */
    private String bizType;

    /** 触发事件：audit */
    private String event;

    private Long subjectId;

    /** debit 借 / credit 贷 */
    private String direction;

    private String summary;

    /** M2 固定 TOTAL=单据总额 */
    private String amountSource;

    private Integer enabled;
}

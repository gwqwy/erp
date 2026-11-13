package cn.tiangong.module.crm.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 销售线索。status：1 待跟进 / 2 已转商机 / 3 无效。
 * 跟进记录复用 biz_chatter（biz_type = crm_lead）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead")
public class CrmLeadDO extends BaseDO {

    private String name;

    /** 来源：电话 / 网络 / 转介绍 / 展会 等 */
    private String source;

    private String contact;

    private String phone;

    private Integer status;

    private Long ownerUserId;

    private String remark;
}

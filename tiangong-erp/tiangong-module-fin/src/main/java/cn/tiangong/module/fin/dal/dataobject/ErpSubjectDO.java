package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会计科目（M2 平面科目表，满足凭证需要；层级化留 M3）。
 * direction：1 借方科目（资产/成本），-1 贷方科目（负债/权益/收入）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_subject")
public class ErpSubjectDO extends BaseDO {

    private String no;

    private String name;

    private Integer direction;

    private Integer status;
}

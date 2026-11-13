package cn.tiangong.module.infra.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单数据：按表单定义填写的一条记录，data JSON = {prop: value}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_form_data")
public class InfraFormDataDO extends BaseDO {

    private Long formDefId;

    private String dataJson;
}

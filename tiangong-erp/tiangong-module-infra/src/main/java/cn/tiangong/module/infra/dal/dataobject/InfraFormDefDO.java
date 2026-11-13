package cn.tiangong.module.infra.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表单定义：fields JSON = [{prop,label,type(input/number/date/select/textarea),required,options[]}]。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("infra_form_def")
public class InfraFormDefDO extends BaseDO {

    private String name;

    private String fieldsJson;

    private Integer status;
}

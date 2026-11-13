import fs from 'node:fs';
import path from 'node:path';

const base = 'E:/文件/编程文件/erp/tiangong-erp';

// 生成 DO + Mapper 的配置：模块、类名、表名、字段块、是否 BaseDO
const defs = [
  // [模块, 类名, 表名, 字段, 是否BaseDO]
  ['bas', 'ErpLocationDO', 'erp_location', `
    private Long warehouseId;

    private String no;

    private String name;

    private Integer status;`, true],
  ['fin', 'ErpFinanceInvoiceDO', 'erp_finance_invoice', `
    private String no;

    /** sale 销项 / purchase 进项 */
    private String type;

    private String invoiceNo;

    private LocalDate invoiceDate;

    private Long orderId;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private String remark;`, true],
  ['bas', 'ErpPartnerPriceDO', 'erp_partner_price', `
    private Long partnerId;

    private Long productId;

    private BigDecimal price;

    private String remark;`, true],
  ['system', 'SystemMessageDO', 'system_message', `
    private Long userId;

    private String title;

    private String content;

    /** false 未读 / true 已读 */
    private Boolean readFlag;`, false],
  ['hr', 'HrRecruitDO', 'hr_recruit', `
    private String no;

    private String position;

    private Integer headcount;

    private String department;

    private Integer status;

    private String remark;`, true],
  ['hr', 'HrTrainingDO', 'hr_training', `
    private String no;

    private String title;

    private String trainer;

    private LocalDate trainDate;

    private String employees;

    private String remark;`, true],
  ['hr', 'HrPerformanceDO', 'hr_performance', `
    private String no;

    private Long employeeId;

    private String period;

    private BigDecimal score;

    private String grade;

    private String remark;`, true],
  ['fin', 'ErpReceivablePlanDO', 'erp_finance_receivable_plan', `
    private String no;

    private Long partnerId;

    private LocalDate planDate;

    private BigDecimal amount;

    private Integer status;

    private String remark;`, true]
];

const header = `import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
`;

const noBaseHeader = `import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

`;

for (const [mod, cls, table, fields, isBase] of defs) {
  const dir = path.join(base, `tiangong-module-${mod}`, 'src', 'main', 'java', 'cn', 'tiangong', 'module', mod, 'dal', 'dataobject');
  fs.mkdirSync(dir, { recursive: true });
  const pkg = `package cn.tiangong.module.${mod}.dal.dataobject;\n\n`;
  let content;
  if (isBase) {
    content = pkg + header + `@Data\n@EqualsAndHashCode(callSuper = true)\n@TableName("${table}")\npublic class ${cls} extends BaseDO {\n${fields}\n}\n`;
  } else {
    content = pkg + noBaseHeader + `@Data\n@TableName("${table}")\npublic class ${cls} {\n\n    @TableId(type = IdType.AUTO)\n    private Long id;\n${fields}\n\n    private LocalDateTime createTime;\n}\n`;
  }
  fs.writeFileSync(path.join(dir, cls + '.java'), content, 'utf8');

  const mapperDir = path.join(base, `tiangong-module-${mod}`, 'src', 'main', 'java', 'cn', 'tiangong', 'module', mod, 'dal', 'mysql');
  fs.mkdirSync(mapperDir, { recursive: true });
  const mapper = `package cn.tiangong.module.${mod}.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.${mod}.dal.dataobject.${cls};

public interface ${cls.replace('DO', 'Mapper')} extends BaseMapperX<${cls}> {
}
`;
  fs.writeFileSync(path.join(mapperDir, cls.replace('DO', 'Mapper') + '.java'), mapper, 'utf8');
  console.log('generated:', mod, cls);
}

// SystemMessageDO 特殊（含 readFlag Boolean）
const msgPath = path.join(base, 'tiangong-module-system', 'src', 'main', 'java', 'cn', 'tiangong', 'module', 'system', 'dal', 'dataobject', 'SystemMessageDO.java');
const msgContent = `package cn.tiangong.module.system.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息：定向发送给指定用户。readFlag false 未读 / true 已读。
 */
@Data
@TableName("system_message")
public class SystemMessageDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private Boolean readFlag;

    private String creator;

    private LocalDateTime createTime;
}
`;
fs.writeFileSync(msgPath, msgContent, 'utf8');
console.log('overwrote SystemMessageDO');
console.log('done');

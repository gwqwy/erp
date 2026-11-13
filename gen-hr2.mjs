import fs from 'node:fs';
import path from 'node:path';

const doBase = 'E:/文件/编程文件/erp/tiangong-erp/tiangong-module-hr/src/main/java/cn/tiangong/module/hr/dal/dataobject';
const mapperBase = 'E:/文件/编程文件/erp/tiangong-erp/tiangong-module-hr/src/main/java/cn/tiangong/module/hr/dal/mysql';

const dos = {
  HrAttendanceDO: {
    table: 'hr_attendance', extra: `
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 考勤记录：每日一条（employee + date 唯一）。status：0 正常 1 迟到 2 早退 3 缺勤 4 事假 5 年假。
 */
`, fields: `
    private Long employeeId;

    private LocalDate attDate;

    private LocalTime checkIn;

    private LocalTime checkOut;

    private Integer status;

    private String remark;
`},
  HrOvertimeDO: {
    table: 'hr_overtime', extra: `
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 加班申请（走审批流 biz_type=hr_overtime）。
 */
`, fields: `
    private String no;

    private Long employeeId;

    private LocalDate otDate;

    private BigDecimal hours;

    private String reason;

    private Integer status;
`},
  HrTripDO: {
    table: 'hr_trip', extra: `
import java.time.LocalDate;

/**
 * 出差申请（走审批流 biz_type=hr_trip）。
 */
`, fields: `
    private String no;

    private Long employeeId;

    private LocalDate startDate;

    private LocalDate endDate;

    private String destination;

    private String reason;

    private Integer status;
`},
  HrResignationDO: {
    table: 'hr_resignation', extra: `
import java.time.LocalDate;

/**
 * 离职申请（走审批流 biz_type=hr_resign；通过后自动改员工状态为离职）。
 */
`, fields: `
    private String no;

    private Long employeeId;

    private LocalDate applyDate;

    private LocalDate planLeaveDate;

    private String reason;

    private Integer status;
`},
  HrRewardPenaltyDO: {
    table: 'hr_reward_penalty', extra: `
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 奖惩记录：type reward 奖励 / penalty 处罚（算薪时取正负号）。
 */
`, fields: `
    private String no;

    private Long employeeId;

    private String type;

    private BigDecimal amount;

    private LocalDate rpDate;

    private String reason;
`},
  HrContractDO: {
    table: 'hr_contract', extra: `
import java.time.LocalDate;

/**
 * 劳动合同。status：0 履行中 1 已到期 2 已解除。
 */
`, fields: `
    private String no;

    private Long employeeId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;

    private String remark;
`}
};

for (const [name, { table, extra, fields }] of Object.entries(dos)) {
  const content = `package cn.tiangong.module.hr.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
${extra}
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("${table}")
public class ${name} extends BaseDO {
${fields}
}
`;
  fs.writeFileSync(path.join(doBase, name + '.java'), content, 'utf8');
  const mapper = `package cn.tiangong.module.hr.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.hr.dal.dataobject.${name};

public interface ${name.replace('DO', 'Mapper')} extends BaseMapperX<${name}> {
}
`;
  fs.writeFileSync(path.join(mapperBase, name.replace('DO', 'Mapper') + '.java'), mapper, 'utf8');
  console.log('generated:', name);
}
console.log('done');

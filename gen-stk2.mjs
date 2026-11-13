import fs from 'node:fs';
import path from 'node:path';

const doBase = 'E:/文件/编程文件/erp/tiangong-erp/tiangong-module-stk/src/main/java/cn/tiangong/module/stk/dal/dataobject';
const mapperBase = 'E:/文件/编程文件/erp/tiangong-erp/tiangong-module-stk/src/main/java/cn/tiangong/module/stk/dal/mysql';

const header = `import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

`;

const dos = {
  StockTransferDO: {
    table: 'erp_stock_transfer', fields: `
    private String no;

    private Long fromWarehouseId;

    private Long toWarehouseId;

    private LocalDate transferDate;

    private BigDecimal totalQty;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
`},
  StockTransferItemDO: {
    table: 'erp_stock_transfer_item', fields: `
    private Long transferId;

    private Long productId;

    private BigDecimal qty;

    private String remark;
`},
  StockCheckDO: {
    table: 'erp_stock_check', fields: `
    private String no;

    private Long warehouseId;

    private LocalDate checkDate;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
`},
  StockCheckItemDO: {
    table: 'erp_stock_check_item', fields: `
    private Long checkId;

    private Long productId;

    private BigDecimal bookQty;

    private BigDecimal realQty;
`},
  SaleReturnDO: {
    table: 'erp_sale_return', fields: `
    private String no;

    private Long customerId;

    private Long warehouseId;

    private Long orderId;

    private LocalDate returnDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
`},
  SaleReturnItemDO: {
    table: 'erp_sale_return_item', fields: `
    private Long returnId;

    private Long productId;

    private BigDecimal qty;

    private BigDecimal price;

    private String remark;
`},
  PurchaseReturnDO: {
    table: 'erp_purchase_return', fields: `
    private String no;

    private Long supplierId;

    private Long warehouseId;

    private Long orderId;

    private LocalDate returnDate;

    private BigDecimal totalQty;

    private BigDecimal totalAmount;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
`},
  PurchaseReturnItemDO: {
    table: 'erp_purchase_return_item', fields: `
    private Long returnId;

    private Long productId;

    private BigDecimal qty;

    private BigDecimal price;

    private String remark;
`},
  StockOtherDO: {
    table: 'erp_stock_other', fields: `
    private String no;

    /** other_in / other_out */
    private String otherType;

    private Long warehouseId;

    private LocalDate otherDate;

    private BigDecimal totalQty;

    private String remark;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime auditTime;
`},
  StockOtherItemDO: {
    table: 'erp_stock_other_item', fields: `
    private Long otherId;

    private Long productId;

    private BigDecimal qty;

    private BigDecimal price;

    private String remark;
`}
};

for (const [name, { table, fields }] of Object.entries(dos)) {
  const content = `package cn.tiangong.module.stk.dal.dataobject;

${header}
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("${table}")
public class ${name} extends BaseDO {
${fields}
}
`;
  fs.writeFileSync(path.join(doBase, name + '.java'), content, 'utf8');
  const mapper = `package cn.tiangong.module.stk.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.stk.dal.dataobject.${name};

public interface ${name.replace('DO', 'Mapper')} extends BaseMapperX<${name}> {
}
`;
  fs.writeFileSync(path.join(mapperBase, name.replace('DO', 'Mapper') + '.java'), mapper, 'utf8');
  console.log('generated:', name);
}
console.log('done');

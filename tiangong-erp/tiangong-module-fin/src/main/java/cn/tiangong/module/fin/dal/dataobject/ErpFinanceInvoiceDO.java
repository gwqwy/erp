package cn.tiangong.module.fin.dal.dataobject;

import cn.tiangong.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_finance_invoice")
public class ErpFinanceInvoiceDO extends BaseDO {

    private String no;

    /** sale 销项 / purchase 进项 */
    private String type;

    private String invoiceNo;

    private LocalDate invoiceDate;

    private Long orderId;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private String remark;
}

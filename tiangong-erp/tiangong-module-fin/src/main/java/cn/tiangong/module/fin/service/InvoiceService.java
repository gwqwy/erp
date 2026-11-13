package cn.tiangong.module.fin.service;

import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceInvoiceDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 发票登记（台账型）：销项/进项发票登记与查询，可关联订单。
 */
@RequiredArgsConstructor
@Service
public class InvoiceService {

    private final ErpFinanceInvoiceMapper invoiceMapper;
    private final SerialNoApi serialNoApi;

    public List<ErpFinanceInvoiceDO> getList(String type, String invoiceNo) {
        return invoiceMapper.selectList(new LambdaQueryWrapper<ErpFinanceInvoiceDO>()
                .eq(type != null && !type.isBlank(), ErpFinanceInvoiceDO::getType, type)
                .like(invoiceNo != null && !invoiceNo.isBlank(), ErpFinanceInvoiceDO::getInvoiceNo, invoiceNo)
                .orderByDesc(ErpFinanceInvoiceDO::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(String type, String invoiceNo, LocalDate invoiceDate, Long orderId,
                       BigDecimal amount, BigDecimal taxAmount, String remark) {
        if (type == null || type.isBlank() || invoiceNo == null || invoiceNo.isBlank()
                || amount == null || amount.signum() <= 0) {
            throw of(8_07, 1, "类型/发票号/金额不合法");
        }
        ErpFinanceInvoiceDO inv = new ErpFinanceInvoiceDO();
        inv.setNo(serialNoApi.nextNo("finance_invoice"));
        inv.setType(type);
        inv.setInvoiceNo(invoiceNo);
        inv.setInvoiceDate(invoiceDate != null ? invoiceDate : LocalDate.now());
        inv.setOrderId(orderId);
        inv.setAmount(amount);
        inv.setTaxAmount(taxAmount == null ? BigDecimal.ZERO : taxAmount);
        inv.setRemark(remark);
        invoiceMapper.insert(inv);
        return inv.getId();
    }

    public void delete(Long id) {
        if (invoiceMapper.selectById(id) == null) {
            throw of(8_07, 2, "发票登记不存在");
        }
        invoiceMapper.deleteById(id);
    }
}

package cn.tiangong.module.fin.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePaymentDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePaymentItemDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PaymentService {

    PageResult<ErpFinancePaymentDO> getPaymentPage(PageParam pageParam, String no, Long partnerId, Integer status);

    PaymentDetail getPaymentDetail(Long id);

    Long createPayment(Long partnerId, Long accountId, LocalDate paymentDate,
                       String remark, List<Item> items);

    void deletePayment(Long id);

    /** 审核：核销应付 + 生成凭证 + 结算账户余额减少 */
    void auditPayment(Long id);

    void voidPayment(Long id);

    /** 红字冲销：已审核付款单——核销退回、红字凭证、账户余额加回 */
    void reversePayment(Long id);

    record Item(Long payableId, BigDecimal amount, String remark) {
    }

    record PaymentDetail(ErpFinancePaymentDO payment, List<ErpFinancePaymentItemDO> items) {
    }
}

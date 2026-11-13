package cn.tiangong.module.fin.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceiptDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceiptItemDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReceiptService {

    PageResult<ErpFinanceReceiptDO> getReceiptPage(PageParam pageParam, String no, Long partnerId, Integer status);

    ReceiptDetail getReceiptDetail(Long id);

    /** 创建收款单：amount 由服务端按 Σ明细计算 */
    Long createReceipt(Long partnerId, Long accountId, LocalDate receiptDate,
                       String remark, List<Item> items);

    void deleteReceipt(Long id);

    /** 审核：核销应收 + 生成凭证 + 结算账户余额增加 */
    void auditReceipt(Long id);

    void voidReceipt(Long id);

    /** 红字冲销：已审核收款单——核销退回、红字凭证、账户余额扣回 */
    void reverseReceipt(Long id);

    record Item(Long receivableId, BigDecimal amount, String remark) {
    }

    record ReceiptDetail(ErpFinanceReceiptDO receipt, List<ErpFinanceReceiptItemDO> items) {
    }
}

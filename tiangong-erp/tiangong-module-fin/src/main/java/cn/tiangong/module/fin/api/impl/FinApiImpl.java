package cn.tiangong.module.fin.api.impl;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.module.fin.api.FinApi;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePayableDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceivableDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinancePayableMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceReceivableMapper;
import cn.tiangong.module.fin.service.VoucherPostingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class FinApiImpl implements FinApi {

    private final ErpFinanceReceivableMapper receivableMapper;
    private final ErpFinancePayableMapper payableMapper;
    private final VoucherPostingService voucherPostingService;
    private final ChatterService chatterService;

    @Override
    public boolean isReceivableClean(String bizType, Long bizId) {
        ErpFinanceReceivableDO ar = receivableMapper.selectOne(new LambdaQueryWrapper<ErpFinanceReceivableDO>()
                .eq(ErpFinanceReceivableDO::getBizType, bizType)
                .eq(ErpFinanceReceivableDO::getBizId, bizId).last("LIMIT 1"));
        return ar != null && ar.getReceivedAmount().signum() == 0;
    }

    @Override
    public boolean isPayableClean(String bizType, Long bizId) {
        ErpFinancePayableDO ap = payableMapper.selectOne(new LambdaQueryWrapper<ErpFinancePayableDO>()
                .eq(ErpFinancePayableDO::getBizType, bizType)
                .eq(ErpFinancePayableDO::getBizId, bizId).last("LIMIT 1"));
        return ap != null && ap.getPaidAmount().signum() == 0;
    }

    @Override
    public void voidReceivableByBiz(String bizType, Long bizId) {
        int rows = receivableMapper.update(null, new LambdaUpdateWrapper<ErpFinanceReceivableDO>()
                .eq(ErpFinanceReceivableDO::getBizType, bizType)
                .eq(ErpFinanceReceivableDO::getBizId, bizId)
                .eq(ErpFinanceReceivableDO::getReceivedAmount, BigDecimal.ZERO)
                .set(ErpFinanceReceivableDO::getStatus, 4));
        if (rows == 0) {
            throw of(8_05, 1, "应收已发生核销，不可随单冲销");
        }
    }

    @Override
    public void voidPayableByBiz(String bizType, Long bizId) {
        int rows = payableMapper.update(null, new LambdaUpdateWrapper<ErpFinancePayableDO>()
                .eq(ErpFinancePayableDO::getBizType, bizType)
                .eq(ErpFinancePayableDO::getBizId, bizId)
                .eq(ErpFinancePayableDO::getPaidAmount, BigDecimal.ZERO)
                .set(ErpFinancePayableDO::getStatus, 4));
        if (rows == 0) {
            throw of(8_05, 2, "应付已发生核销，不可随单冲销");
        }
    }

    @Override
    public BigDecimal unsettledReceivableOfPartner(Long partnerId) {
        return receivableMapper.selectList(new LambdaQueryWrapper<ErpFinanceReceivableDO>()
                        .eq(ErpFinanceReceivableDO::getPartnerId, partnerId)
                        .in(ErpFinanceReceivableDO::getStatus, List.of(1, 2)))
                .stream()
                .map(r -> r.getAmount().subtract(r.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String reverseVoucher(String bizType, Long bizId) {
        return voucherPostingService.reverseBySource(bizType, bizId);
    }
}

package cn.tiangong.module.fin.api.impl;

import cn.tiangong.module.fin.api.FinStatApi;
import cn.tiangong.module.fin.dal.dataobject.ErpFinancePayableDO;
import cn.tiangong.module.fin.dal.dataobject.ErpFinanceReceivableDO;
import cn.tiangong.module.fin.dal.mysql.ErpFinancePayableMapper;
import cn.tiangong.module.fin.dal.mysql.ErpFinanceReceivableMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Service
public class FinStatApiImpl implements FinStatApi {

    private final ErpFinanceReceivableMapper receivableMapper;
    private final ErpFinancePayableMapper payableMapper;

    @Override
    public BigDecimal unsettledReceivable() {
        List<ErpFinanceReceivableDO> list = receivableMapper.selectList(
                new LambdaQueryWrapper<ErpFinanceReceivableDO>()
                        .in(ErpFinanceReceivableDO::getStatus, List.of(1, 2)));
        return list.stream()
                .map(r -> r.getAmount().subtract(r.getReceivedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal unsettledPayable() {
        List<ErpFinancePayableDO> list = payableMapper.selectList(
                new LambdaQueryWrapper<ErpFinancePayableDO>()
                        .in(ErpFinancePayableDO::getStatus, List.of(1, 2)));
        return list.stream()
                .map(p -> p.getAmount().subtract(p.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

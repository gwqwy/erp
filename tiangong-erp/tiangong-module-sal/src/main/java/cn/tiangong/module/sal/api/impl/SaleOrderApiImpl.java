package cn.tiangong.module.sal.api.impl;

import cn.tiangong.module.sal.api.SaleOrderApi;
import cn.tiangong.module.sal.service.order.SaleOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SaleOrderApiImpl implements SaleOrderApi {

    private final SaleOrderService saleOrderService;

    @Override
    public Long createDraftOrder(Long customerId, String remark, List<Item> items) {
        List<SaleOrderService.Item> params = items.stream()
                .map(i -> new SaleOrderService.Item(i.productId(), i.unitId(), i.qty(),
                        i.price() == null ? BigDecimal.ZERO : i.price(), BigDecimal.ZERO, i.remark()))
                .toList();
        return saleOrderService.createOrder(customerId, LocalDate.now(), null, remark, params);
    }
}

package cn.tiangong.module.pur.api.impl;

import cn.tiangong.module.pur.api.PurOrderApi;
import cn.tiangong.module.pur.service.order.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PurOrderApiImpl implements PurOrderApi {

    private final PurchaseOrderService purchaseOrderService;

    @Override
    public Long createDraftOrder(Long supplierId, String remark, List<Item> items) {
        List<PurchaseOrderService.Item> params = items.stream()
                .map(i -> new PurchaseOrderService.Item(i.productId(), i.unitId(), i.qty(),
                        i.price() == null ? BigDecimal.ZERO : i.price(), BigDecimal.ZERO, i.remark()))
                .toList();
        return purchaseOrderService.createOrder(supplierId, LocalDate.now(), null, remark, params);
    }
}

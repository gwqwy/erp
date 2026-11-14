package cn.tiangong.module.pur.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseOrderDO;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseOrderMapper;
import cn.tiangong.module.pur.service.order.PurchaseOrderService;
import cn.tiangong.framework.web.core.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** 采购订金：登记（累计）/ 查询。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/pur/order")
public class PurchaseOrderDepositController {

    private final ErpPurchaseOrderMapper orderMapper;
    private final PurchaseOrderService orderService;

    @GetMapping("/deposit")
    @PreAuthorize("@ss.hasPermission('pur:order:query')")
    public CommonResult<ErpPurchaseOrderDO> getDeposit(@RequestParam("id") Long id) {
        return CommonResult.success(orderMapper.selectById(id));
    }

    @PostMapping("/deposit")
    @PreAuthorize("@ss.hasPermission('pur:order:audit')")
    public CommonResult<BigDecimal> registerDeposit(@RequestParam("id") Long id,
                                                    @RequestParam("amount") BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ServiceException.of(6_02, 1, "订金金额必须大于 0");
        }
        ErpPurchaseOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw ServiceException.of(6_02, 2, "订单不存在");
        }
        BigDecimal total = order.getTotalAmountTax() == null ? order.getTotalAmount() : order.getTotalAmountTax();
        BigDecimal newDeposit = order.getDepositAmount().add(amount);
        if (newDeposit.compareTo(total) > 0) {
            throw ServiceException.of(6_02, 3, "累计订金 " + newDeposit + " 超过订单总额 " + total);
        }
        orderService.updateDeposit(id, newDeposit);
        return CommonResult.success(newDeposit);
    }
}

package cn.tiangong.module.pur.service.returnorder;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.fin.api.dto.FinanceEvent;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseReturnDO;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseReturnItemDO;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseReturnItemMapper;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseReturnMapper;
import cn.tiangong.module.stk.api.StockPostingApi;
import cn.tiangong.module.stk.api.dto.MoveCommand;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 采购退货：审核后货物出库（走库存引擎），凭证经事件生成（冲减应付）。
 */
@RequiredArgsConstructor
@Service
public class PurchaseReturnService {

    public static final String BIZ_TYPE = "pur_return";
    private static final String MOVE_BIZ_TYPE = "purchase_return";

    private final ErpPurchaseReturnMapper returnMapper;
    private final ErpPurchaseReturnItemMapper returnItemMapper;
    private final SerialNoApi serialNoApi;
    private final StockPostingApi stockPostingApi;
    private final ChatterService chatterService;
    private final ApplicationEventPublisher eventPublisher;

    public PageResult<ErpPurchaseReturnDO> getReturnPage(PageParam pageParam, String no, Long supplierId, Integer status) {
        return returnMapper.selectPage(pageParam, new LambdaQueryWrapper<ErpPurchaseReturnDO>()
                .like(no != null && !no.isBlank(), ErpPurchaseReturnDO::getNo, no)
                .eq(supplierId != null, ErpPurchaseReturnDO::getSupplierId, supplierId)
                .eq(status != null, ErpPurchaseReturnDO::getStatus, status)
                .orderByDesc(ErpPurchaseReturnDO::getId));
    }

    public ReturnDetail getReturnDetail(Long id) {
        ErpPurchaseReturnDO ret = getReturn(id);
        return new ReturnDetail(ret, returnItemMapper.selectList(
                new LambdaQueryWrapper<ErpPurchaseReturnItemDO>()
                        .eq(ErpPurchaseReturnItemDO::getReturnId, id)
                        .orderByAsc(ErpPurchaseReturnItemDO::getId)));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createReturn(Long supplierId, Long warehouseId, LocalDate returnDate,
                             String remark, List<Item> items) {
        validate(supplierId, warehouseId, items);
        ErpPurchaseReturnDO ret = new ErpPurchaseReturnDO();
        ret.setNo(serialNoApi.nextNo("purchase_return"));
        ret.setSupplierId(supplierId);
        ret.setWarehouseId(warehouseId);
        ret.setReturnDate(returnDate != null ? returnDate : LocalDate.now());
        ret.setTotalQty(items.stream().map(Item::qty).reduce(BigDecimal.ZERO, BigDecimal::add));
        ret.setTotalAmount(items.stream().map(i -> i.qty().multiply(i.price()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        ret.setRemark(remark);
        ret.setStatus(BizOrderStatus.DRAFT.getValue());
        returnMapper.insert(ret);
        for (Item item : items) {
            ErpPurchaseReturnItemDO itemDO = new ErpPurchaseReturnItemDO();
            itemDO.setReturnId(ret.getId());
            itemDO.setProductId(item.productId());
            itemDO.setQty(item.qty());
            itemDO.setPrice(item.price());
            itemDO.setRemark(item.remark());
            returnItemMapper.insert(itemDO);
        }
        chatterService.trace(BIZ_TYPE, ret.getId(), "创建采购退货单 " + ret.getNo(), null);
        return ret.getId();
    }

    /** 审核：货物出库（退回供应商）+ 生成冲减应付凭证 */
    @Transactional(rollbackFor = Exception.class)
    public void auditReturn(Long id) {
        ErpPurchaseReturnDO ret = getReturn(id);
        OrderStateChecker.require(ret.getStatus(), ret.getNo(), BizOrderStatus.DRAFT);
        List<ErpPurchaseReturnItemDO> items = returnItemMapper.selectList(
                new LambdaQueryWrapper<ErpPurchaseReturnItemDO>()
                        .eq(ErpPurchaseReturnItemDO::getReturnId, id));
        List<MoveCommand> commands = new ArrayList<>();
        for (ErpPurchaseReturnItemDO item : items) {
            commands.add(new MoveCommand(MOVE_BIZ_TYPE, id, ret.getNo(), -1,
                    ret.getWarehouseId(), item.getProductId(), item.getQty(),
                    null, ret.getSupplierId(), null));
        }
        stockPostingApi.submit(commands);
        ret.setStatus(BizOrderStatus.AUDITED.getValue());
        ret.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        ret.setAuditTime(LocalDateTime.now());
        returnMapper.updateById(ret);
        chatterService.trace(BIZ_TYPE, id, String.format("审核采购退货单 %s，货物已退库出账", ret.getNo()), null);
        eventPublisher.publishEvent(new FinanceEvent("purchase_return", "audit", BIZ_TYPE,
                ret.getId(), ret.getNo(), ret.getSupplierId(), ret.getTotalAmount(), null, ret.getReturnDate()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidReturn(Long id) {
        ErpPurchaseReturnDO ret = getReturn(id);
        OrderStateChecker.require(ret.getStatus(), ret.getNo(), BizOrderStatus.DRAFT);
        ret.setStatus(BizOrderStatus.VOIDED.getValue());
        returnMapper.updateById(ret);
        chatterService.trace(BIZ_TYPE, id, "作废采购退货单 " + ret.getNo(), null);
    }

    public void deleteReturn(Long id) {
        ErpPurchaseReturnDO ret = getReturn(id);
        OrderStateChecker.require(ret.getStatus(), ret.getNo(), BizOrderStatus.DRAFT);
        returnMapper.deleteById(id);
        returnItemMapper.delete(new LambdaQueryWrapper<ErpPurchaseReturnItemDO>()
                .eq(ErpPurchaseReturnItemDO::getReturnId, id));
    }

    private void validate(Long supplierId, Long warehouseId, List<Item> items) {
        if (supplierId == null || warehouseId == null) {
            throw of(6_02, 1, "供应商与仓库不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw of(6_02, 2, "退货明细不能为空");
        }
    }

    private ErpPurchaseReturnDO getReturn(Long id) {
        ErpPurchaseReturnDO ret = returnMapper.selectById(id);
        if (ret == null) {
            throw of(6_02, 3, "采购退货单不存在");
        }
        return ret;
    }

    public record Item(Long productId, BigDecimal qty, BigDecimal price, String remark) {
    }

    public record ReturnDetail(ErpPurchaseReturnDO ret, List<ErpPurchaseReturnItemDO> items) {
    }
}

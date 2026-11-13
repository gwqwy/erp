package cn.tiangong.module.sal.service.returnorder;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.biz.common.order.BizOrderStatus;
import cn.tiangong.framework.biz.common.order.OrderStateChecker;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.fin.api.dto.FinanceEvent;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleReturnDO;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleReturnItemDO;
import cn.tiangong.module.sal.dal.mysql.ErpSaleReturnItemMapper;
import cn.tiangong.module.sal.dal.mysql.ErpSaleReturnMapper;
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
 * 销售退货：审核后货物回库（走库存引擎），凭证经事件生成（冲减收入）。
 */
@RequiredArgsConstructor
@Service
public class SaleReturnService {

    public static final String BIZ_TYPE = "sal_return";
    private static final String MOVE_BIZ_TYPE = "sale_return";

    private final ErpSaleReturnMapper returnMapper;
    private final ErpSaleReturnItemMapper returnItemMapper;
    private final SerialNoApi serialNoApi;
    private final StockPostingApi stockPostingApi;
    private final ChatterService chatterService;
    private final ApplicationEventPublisher eventPublisher;

    public PageResult<ErpSaleReturnDO> getReturnPage(PageParam pageParam, String no, Long customerId, Integer status) {
        return returnMapper.selectPage(pageParam, new LambdaQueryWrapper<ErpSaleReturnDO>()
                .like(no != null && !no.isBlank(), ErpSaleReturnDO::getNo, no)
                .eq(customerId != null, ErpSaleReturnDO::getCustomerId, customerId)
                .eq(status != null, ErpSaleReturnDO::getStatus, status)
                .orderByDesc(ErpSaleReturnDO::getId));
    }

    public ReturnDetail getReturnDetail(Long id) {
        ErpSaleReturnDO ret = getReturn(id);
        return new ReturnDetail(ret, returnItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleReturnItemDO>()
                        .eq(ErpSaleReturnItemDO::getReturnId, id)
                        .orderByAsc(ErpSaleReturnItemDO::getId)));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createReturn(Long customerId, Long warehouseId, LocalDate returnDate,
                             String remark, List<Item> items) {
        validate(customerId, warehouseId, items);
        ErpSaleReturnDO ret = new ErpSaleReturnDO();
        ret.setNo(serialNoApi.nextNo("sale_return"));
        ret.setCustomerId(customerId);
        ret.setWarehouseId(warehouseId);
        ret.setReturnDate(returnDate != null ? returnDate : LocalDate.now());
        ret.setTotalQty(items.stream().map(Item::qty).reduce(BigDecimal.ZERO, BigDecimal::add));
        ret.setTotalAmount(items.stream().map(i -> i.qty().multiply(i.price()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        ret.setRemark(remark);
        ret.setStatus(BizOrderStatus.DRAFT.getValue());
        returnMapper.insert(ret);
        for (Item item : items) {
            ErpSaleReturnItemDO itemDO = new ErpSaleReturnItemDO();
            itemDO.setReturnId(ret.getId());
            itemDO.setProductId(item.productId());
            itemDO.setQty(item.qty());
            itemDO.setPrice(item.price());
            itemDO.setRemark(item.remark());
            returnItemMapper.insert(itemDO);
        }
        chatterService.trace(BIZ_TYPE, ret.getId(), "创建销售退货单 " + ret.getNo(), null);
        return ret.getId();
    }

    /** 审核：货物回库 + 生成冲减收入凭证 */
    @Transactional(rollbackFor = Exception.class)
    public void auditReturn(Long id) {
        ErpSaleReturnDO ret = getReturn(id);
        OrderStateChecker.require(ret.getStatus(), ret.getNo(), BizOrderStatus.DRAFT);
        List<ErpSaleReturnItemDO> items = returnItemMapper.selectList(
                new LambdaQueryWrapper<ErpSaleReturnItemDO>()
                        .eq(ErpSaleReturnItemDO::getReturnId, id));
        List<MoveCommand> commands = new ArrayList<>();
        for (ErpSaleReturnItemDO item : items) {
            commands.add(new MoveCommand(MOVE_BIZ_TYPE, id, ret.getNo(), 1,
                    ret.getWarehouseId(), item.getProductId(), item.getQty(),
                    item.getPrice(), ret.getCustomerId(), null));
        }
        stockPostingApi.submit(commands);
        ret.setStatus(BizOrderStatus.AUDITED.getValue());
        ret.setAuditUserId(SecurityFrameworkUtils.getLoginUserId());
        ret.setAuditTime(LocalDateTime.now());
        returnMapper.updateById(ret);
        chatterService.trace(BIZ_TYPE, id, String.format("审核销售退货单 %s，货物已回库", ret.getNo()), null);
        eventPublisher.publishEvent(new FinanceEvent("sale_return", "audit", BIZ_TYPE,
                ret.getId(), ret.getNo(), ret.getCustomerId(), ret.getTotalAmount(), null, ret.getReturnDate()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidReturn(Long id) {
        ErpSaleReturnDO ret = getReturn(id);
        OrderStateChecker.require(ret.getStatus(), ret.getNo(), BizOrderStatus.DRAFT);
        ret.setStatus(BizOrderStatus.VOIDED.getValue());
        returnMapper.updateById(ret);
        chatterService.trace(BIZ_TYPE, id, "作废销售退货单 " + ret.getNo(), null);
    }

    public void deleteReturn(Long id) {
        ErpSaleReturnDO ret = getReturn(id);
        OrderStateChecker.require(ret.getStatus(), ret.getNo(), BizOrderStatus.DRAFT);
        returnMapper.deleteById(id);
        returnItemMapper.delete(new LambdaQueryWrapper<ErpSaleReturnItemDO>()
                .eq(ErpSaleReturnItemDO::getReturnId, id));
    }

    private void validate(Long customerId, Long warehouseId, List<Item> items) {
        if (customerId == null || warehouseId == null) {
            throw of(7_02, 1, "客户与仓库不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw of(7_02, 2, "退货明细不能为空");
        }
    }

    private ErpSaleReturnDO getReturn(Long id) {
        ErpSaleReturnDO ret = returnMapper.selectById(id);
        if (ret == null) {
            throw of(7_02, 3, "销售退货单不存在");
        }
        return ret;
    }

    public record Item(Long productId, BigDecimal qty, BigDecimal price, String remark) {
    }

    public record ReturnDetail(ErpSaleReturnDO ret, List<ErpSaleReturnItemDO> items) {
    }
}

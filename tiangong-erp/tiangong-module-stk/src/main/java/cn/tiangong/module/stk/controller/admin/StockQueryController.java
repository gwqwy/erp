package cn.tiangong.module.stk.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.ProductApi;
import cn.tiangong.module.bas.api.WarehouseApi;
import cn.tiangong.module.stk.dal.dataobject.StockMoveDO;
import cn.tiangong.module.stk.dal.dataobject.StockQuantDO;
import cn.tiangong.module.stk.dal.mysql.StockMoveMapper;
import cn.tiangong.module.stk.dal.mysql.StockQuantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存查询：即时库存（quant）与库存流水（move）。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/stk")
public class StockQueryController {

    private final StockQuantMapper quantMapper;
    private final StockMoveMapper moveMapper;
    private final ProductApi productApi;
    private final WarehouseApi warehouseApi;

    @GetMapping("/quant/page")
    @PreAuthorize("@ss.hasPermission('stk:quant:query')")
    public CommonResult<PageResult<QuantRespVO>> getQuantPage(PageParam pageParam,
                                                              @RequestParam(required = false) Long productId,
                                                              @RequestParam(required = false) Long warehouseId) {
        LambdaQueryWrapper<StockQuantDO> wrapper = new LambdaQueryWrapper<StockQuantDO>()
                .eq(productId != null, StockQuantDO::getProductId, productId)
                .eq(warehouseId != null, StockQuantDO::getWarehouseId, warehouseId)
                .gt(StockQuantDO::getQty, 0)
                .orderByDesc(StockQuantDO::getQty);
        PageResult<StockQuantDO> page = quantMapper.selectPage(pageParam, wrapper);
        Map<Long, String> productNames = productApi.getProductNames(
                page.getList().stream().map(StockQuantDO::getProductId).collect(Collectors.toSet()));
        Map<Long, String> warehouseNames = warehouseApi.getWarehouseNames(
                page.getList().stream().map(StockQuantDO::getWarehouseId).collect(Collectors.toSet()));
        return CommonResult.success(PageResult.of(page, q -> QuantRespVO.of(q, productNames, warehouseNames)));
    }

    @GetMapping("/move/page")
    @PreAuthorize("@ss.hasPermission('stk:move:query')")
    public CommonResult<PageResult<MoveRespVO>> getMovePage(PageParam pageParam,
                                                            @RequestParam(required = false) String bizNo,
                                                            @RequestParam(required = false) Long productId,
                                                            @RequestParam(required = false) Integer direction) {
        LambdaQueryWrapper<StockMoveDO> wrapper = new LambdaQueryWrapper<StockMoveDO>()
                .like(bizNo != null && !bizNo.isBlank(), StockMoveDO::getBizNo, bizNo)
                .eq(productId != null, StockMoveDO::getProductId, productId)
                .eq(direction != null, StockMoveDO::getDirection, direction)
                .orderByDesc(StockMoveDO::getId);
        PageResult<StockMoveDO> page = moveMapper.selectPage(pageParam, wrapper);
        Map<Long, String> productNames = productApi.getProductNames(
                page.getList().stream().map(StockMoveDO::getProductId).collect(Collectors.toSet()));
        Map<Long, String> warehouseNames = warehouseApi.getWarehouseNames(
                page.getList().stream().map(StockMoveDO::getWarehouseId).collect(Collectors.toSet()));
        return CommonResult.success(PageResult.of(page, m -> MoveRespVO.of(m, productNames, warehouseNames)));
    }

    public record QuantRespVO(String id, String productId, String productName, String warehouseId,
                              String warehouseName, String batchNo, java.math.BigDecimal qty,
                              java.math.BigDecimal lockedQty, java.math.BigDecimal avgCost,
                              java.math.BigDecimal amount) {
        static QuantRespVO of(StockQuantDO q, Map<Long, String> pn, Map<Long, String> wn) {
            return new QuantRespVO(String.valueOf(q.getId()), String.valueOf(q.getProductId()),
                    pn.getOrDefault(q.getProductId(), "未知产品"), String.valueOf(q.getWarehouseId()),
                    wn.getOrDefault(q.getWarehouseId(), "未知仓库"), q.getBatchNo(), q.getQty(),
                    q.getLockedQty(), q.getAvgCost(),
                    q.getQty().multiply(q.getAvgCost() == null ? java.math.BigDecimal.ZERO : q.getAvgCost()));
        }
    }

    public record MoveRespVO(String id, String moveNo, String bizType, String bizId, String bizNo, Integer direction,
                             String productName, String warehouseName, String batchNo, java.math.BigDecimal qty,
                             java.math.BigDecimal price, java.math.BigDecimal amount,
                             String moveTime) {
        static MoveRespVO of(StockMoveDO m, Map<Long, String> pn, Map<Long, String> wn) {
            return new MoveRespVO(String.valueOf(m.getId()), m.getMoveNo(), m.getBizType(),
                    String.valueOf(m.getBizId()), m.getBizNo(),
                    m.getDirection(), pn.getOrDefault(m.getProductId(), "未知产品"),
                    wn.getOrDefault(m.getWarehouseId(), "未知仓库"), m.getBatchNo(), m.getQty(), m.getPrice(),
                    m.getAmount(), String.valueOf(m.getMoveTime()));
        }
    }
}

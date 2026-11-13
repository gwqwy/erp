package cn.tiangong.module.stk.service;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.module.bas.api.ProductApi;
import cn.tiangong.module.stk.api.StockPostingApi;
import cn.tiangong.module.stk.api.dto.MoveCommand;
import cn.tiangong.module.stk.dal.dataobject.StockMoveDO;
import cn.tiangong.module.stk.dal.dataobject.StockQuantDO;
import cn.tiangong.module.stk.dal.mysql.StockMoveMapper;
import cn.tiangong.module.stk.dal.mysql.StockQuantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

/**
 * 库存双账引擎（《02-总体架构》§4.2）：
 * - quant 即时账：维度唯一键 + 乐观锁 version 原子更新，入库刷新移动加权平均成本；
 * - move 流水账：只增不改，记录审计事实（按批次一笔一行）；
 * - 出库校验结存，防止超卖/负库存；
 * - 指定批次按该批次出库；未指定批次且产品多批次时按先进先出（FIFO）自动拆分多笔。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class StockPostingServiceImpl implements StockPostingApi {

    public static final Long DEFAULT_COMPANY = 1L;
    public static final Long DEFAULT_LOCATION = 0L;
    private static final int MAX_RETRY = 3;

    private final StockQuantMapper quantMapper;
    private final StockMoveMapper moveMapper;
    private final ProductApi productApi;
    private final ChatterService chatterService;
    private final StringRedisTemplate redisTemplate;

    /** 单笔过账明细（一个批次一条流水） */
    private record PostedMove(String batchNo, BigDecimal qty, BigDecimal price, BigDecimal amount) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BigDecimal> submit(List<MoveCommand> commands) {
        List<BigDecimal> amounts = new ArrayList<>(commands.size());
        for (MoveCommand command : commands) {
            BigDecimal total = BigDecimal.ZERO;
            for (PostedMove move : post(command)) {
                insertMove(command, move);
                total = total.add(move.amount());
            }
            amounts.add(total);

            String productName = productApi.getProductName(command.productId());
            chatterService.trace(command.bizType(), command.bizId(),
                    String.format("库存过账：%s %s × %s，成本金额 %s",
                            command.direction() == 1 ? "入库" : "出库",
                            productName,
                            command.qty().stripTrailingZeros().toPlainString(),
                            total.stripTrailingZeros().toPlainString()),
                    null);
        }
        return amounts;
    }

    private List<PostedMove> post(MoveCommand command) {
        if (command.qty() == null || command.qty().signum() <= 0) {
            throw of(3_00, 1, "过账数量必须为正数");
        }
        String batchNo = command.batchNo() == null ? "" : command.batchNo();
        if (command.direction() != null && command.direction() == 1) {
            // 产品启用批次管理且未指定批次时，入库自动生成批次号（B + 时间戳 + 序列）
            if (batchNo.isEmpty() && productApi.isBatchEnabled(command.productId())) {
                batchNo = "B" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + String.format("%03d", nextBatchSeq());
            }
            return List.of(postInbound(command, batchNo));
        }
        return postOutbound(command, batchNo);
    }

    /** 批次号序列（Redis 号段） */
    private long nextBatchSeq() {
        String key = "tiangong:serial:batch_no:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1) {
            redisTemplate.expire(key, Duration.ofDays(7));
        }
        return seq == null ? 1L : seq;
    }

    /** 入库：插入或更新 quant，移动加权平均成本 */
    private PostedMove postInbound(MoveCommand command, String batchNo) {
        BigDecimal price = command.price() == null ? BigDecimal.ZERO : command.price();
        StockQuantDO quant = quantMapper.selectByDim(DEFAULT_COMPANY, command.warehouseId(),
                DEFAULT_LOCATION, command.productId(), batchNo);
        if (quant == null) {
            quant = new StockQuantDO();
            quant.setCompanyId(DEFAULT_COMPANY);
            quant.setWarehouseId(command.warehouseId());
            quant.setLocationId(DEFAULT_LOCATION);
            quant.setProductId(command.productId());
            quant.setBatchNo(batchNo);
            quant.setQty(command.qty());
            quant.setLockedQty(BigDecimal.ZERO);
            quant.setAvgCost(price);
            quant.setVersion(0);
            quantMapper.insert(quant);
            return new PostedMove(batchNo, command.qty(), price, command.qty().multiply(price));
        }
        for (int i = 0; i < MAX_RETRY; i++) {
            BigDecimal oldQty = quant.getQty();
            BigDecimal newQty = oldQty.add(command.qty());
            BigDecimal newAvg = newQty.signum() == 0 ? price
                    : oldQty.multiply(quant.getAvgCost()).add(command.qty().multiply(price))
                    .divide(newQty, 6, java.math.RoundingMode.HALF_UP);
            int rows = quantMapper.update(null, new LambdaUpdateWrapper<StockQuantDO>()
                    .eq(StockQuantDO::getId, quant.getId())
                    .eq(StockQuantDO::getVersion, quant.getVersion())
                    .set(StockQuantDO::getQty, newQty)
                    .set(StockQuantDO::getAvgCost, newAvg)
                    .set(StockQuantDO::getVersion, quant.getVersion() + 1));
            if (rows > 0) {
                return new PostedMove(batchNo, command.qty(), price, command.qty().multiply(price));
            }
            quant = quantMapper.selectById(quant.getId());
            if (quant == null) {
                break;
            }
        }
        throw of(3_00, 2, "库存并发冲突，请重试");
    }

    /**
     * 出库：指定批次按该批次出；未指定批次按先进先出自动拆分多笔（每批次一条流水）。
     * 成本取各批次当时的移动加权平均成本。
     */
    private List<PostedMove> postOutbound(MoveCommand command, String batchNo) {
        if (!batchNo.isEmpty()) {
            PostedMove move = postOutboundSingle(command, batchNo);
            return List.of(move);
        }
        // FIFO：按入库时间先后（create_time, id）消耗各批次结存
        List<StockQuantDO> rows = quantMapper.selectList(new LambdaQueryWrapper<StockQuantDO>()
                .eq(StockQuantDO::getCompanyId, DEFAULT_COMPANY)
                .eq(StockQuantDO::getWarehouseId, command.warehouseId())
                .eq(StockQuantDO::getLocationId, DEFAULT_LOCATION)
                .eq(StockQuantDO::getProductId, command.productId())
                .gt(StockQuantDO::getQty, BigDecimal.ZERO)
                .orderByAsc(StockQuantDO::getCreateTime)
                .orderByAsc(StockQuantDO::getId));
        BigDecimal available = rows.stream()
                .map(StockQuantDO::getQty).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (available.compareTo(command.qty()) < 0) {
            String productName = productApi.getProductName(command.productId());
            throw of(3_04, 1, String.format("产品 [%s] 库存不足：可用 %s，需要 %s",
                    productName, available.stripTrailingZeros().toPlainString(),
                    command.qty().stripTrailingZeros().toPlainString()));
        }
        List<PostedMove> moves = new ArrayList<>();
        BigDecimal remaining = command.qty();
        for (StockQuantDO row : rows) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal take = remaining.min(row.getQty());
            moves.add(postOutboundRow(command, row.getBatchNo(), row, take));
            remaining = remaining.subtract(take);
        }
        return moves;
    }

    /** 从指定批次行出库（乐观锁重试） */
    private PostedMove postOutboundRow(MoveCommand command, String batchNo,
                                       StockQuantDO quant, BigDecimal take) {
        for (int i = 0; i < MAX_RETRY; i++) {
            StockQuantDO current = quantMapper.selectById(quant.getId());
            if (current == null || current.getQty().compareTo(take) < 0) {
                BigDecimal avail = current == null ? BigDecimal.ZERO : current.getQty();
                String productName = productApi.getProductName(command.productId());
                throw of(3_04, 1, String.format("产品 [%s] 库存不足：可用 %s，需要 %s",
                        productName, avail.stripTrailingZeros().toPlainString(),
                        take.stripTrailingZeros().toPlainString()));
            }
            BigDecimal cost = current.getAvgCost() == null ? BigDecimal.ZERO : current.getAvgCost();
            int rows = quantMapper.update(null, new LambdaUpdateWrapper<StockQuantDO>()
                    .eq(StockQuantDO::getId, current.getId())
                    .eq(StockQuantDO::getVersion, current.getVersion())
                    .set(StockQuantDO::getQty, current.getQty().subtract(take))
                    .set(StockQuantDO::getVersion, current.getVersion() + 1));
            if (rows > 0) {
                return new PostedMove(batchNo, take, cost, take.multiply(cost));
            }
        }
        throw of(3_00, 2, "库存并发冲突，请重试");
    }

    /** 单批次出库（未启用批次/明确指定批次） */
    private PostedMove postOutboundSingle(MoveCommand command, String batchNo) {
        for (int i = 0; i < MAX_RETRY; i++) {
            StockQuantDO quant = quantMapper.selectByDim(DEFAULT_COMPANY, command.warehouseId(),
                    DEFAULT_LOCATION, command.productId(), batchNo);
            if (quant == null || quant.getQty().compareTo(command.qty()) < 0) {
                BigDecimal available = quant == null ? BigDecimal.ZERO : quant.getQty();
                String productName = productApi.getProductName(command.productId());
                throw of(3_04, 1, String.format("产品 [%s] 库存不足：可用 %s，需要 %s",
                        productName, available.stripTrailingZeros().toPlainString(),
                        command.qty().stripTrailingZeros().toPlainString()));
            }
            BigDecimal cost = quant.getAvgCost() == null ? BigDecimal.ZERO : quant.getAvgCost();
            int rows = quantMapper.update(null, new LambdaUpdateWrapper<StockQuantDO>()
                    .eq(StockQuantDO::getId, quant.getId())
                    .eq(StockQuantDO::getVersion, quant.getVersion())
                    .set(StockQuantDO::getQty, quant.getQty().subtract(command.qty()))
                    .set(StockQuantDO::getVersion, quant.getVersion() + 1));
            if (rows > 0) {
                return new PostedMove(batchNo, command.qty(), cost, command.qty().multiply(cost));
            }
        }
        throw of(3_00, 2, "库存并发冲突，请重试");
    }

    private void insertMove(MoveCommand command, PostedMove move) {
        StockMoveDO moveDo = new StockMoveDO();
        moveDo.setMoveNo(nextMoveNo());
        moveDo.setBizType(command.bizType());
        moveDo.setBizId(command.bizId());
        moveDo.setBizNo(command.bizNo());
        moveDo.setDirection(command.direction());
        moveDo.setWarehouseId(command.warehouseId());
        moveDo.setLocationId(DEFAULT_LOCATION);
        moveDo.setProductId(command.productId());
        moveDo.setUnitId(null);
        moveDo.setQty(command.direction() == 1 ? move.qty() : move.qty().negate());
        moveDo.setPrice(move.amount().divide(move.qty(), 6, java.math.RoundingMode.HALF_UP));
        moveDo.setAmount(move.amount());
        moveDo.setBatchNo(move.batchNo());
        moveDo.setPartnerId(command.partnerId());
        moveDo.setMoveTime(LocalDateTime.now());
        moveDo.setPostUserId(SecurityFrameworkUtils.getLoginUserId());
        moveMapper.insert(moveDo);
    }

    /** 流水号：MV + yyyyMM + 4 位序列（Redis 号段，按月重置；key 新建时跳 1000 防 Redis 重启回退撞号） */
    private String nextMoveNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = "tiangong:serial:stock_move:" + datePart;
        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1) {
            redisTemplate.opsForValue().set(key, "1000", Duration.ofDays(90));
            seq = 1001L;
        } else {
            redisTemplate.expire(key, Duration.ofDays(90));
        }
        return "MV" + datePart + String.format("%04d", seq == null ? 1L : seq);
    }
}

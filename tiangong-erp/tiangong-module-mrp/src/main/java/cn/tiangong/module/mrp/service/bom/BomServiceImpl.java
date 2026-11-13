package cn.tiangong.module.mrp.service.bom;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomDO;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpBomItemDO;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpBomItemMapper;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpBomMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class BomServiceImpl implements BomService {

    public static final String BIZ_TYPE = "mrp_bom";
    public static final int STATUS_ENABLED = 1;
    /** BOM 展开最大深度，防环形引用 */
    private static final int MAX_DEPTH = 10;

    private final ErpMrpBomMapper bomMapper;
    private final ErpMrpBomItemMapper bomItemMapper;
    private final SerialNoApi serialNoApi;
    private final ChatterService chatterService;

    @Override
    public PageResult<ErpMrpBomDO> getBomPage(PageParam pageParam, Long productId, Integer status) {
        LambdaQueryWrapper<ErpMrpBomDO> wrapper = new LambdaQueryWrapper<ErpMrpBomDO>()
                .eq(productId != null, ErpMrpBomDO::getProductId, productId)
                .eq(status != null, ErpMrpBomDO::getStatus, status)
                .orderByDesc(ErpMrpBomDO::getId);
        return bomMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public BomDetail getBomDetail(Long id) {
        ErpMrpBomDO bom = getBom(id);
        return new BomDetail(bom, getItemList(id));
    }

    private List<ErpMrpBomItemDO> getItemList(Long bomId) {
        return bomItemMapper.selectList(new LambdaQueryWrapper<ErpMrpBomItemDO>()
                .eq(ErpMrpBomItemDO::getBomId, bomId)
                .orderByAsc(ErpMrpBomItemDO::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBom(Long productId, Integer version, BigDecimal qty, String remark, List<Item> items) {
        validate(productId, qty, items);
        int v = version != null ? version : 1;
        if (bomMapper.countByProductVersion(productId, v) > 0) {
            throw of(9_00, 5, String.format("该产品已存在版本 %d 的 BOM，请换版本号或先删除", v));
        }
        ErpMrpBomDO bom = new ErpMrpBomDO();
        bom.setNo(serialNoApi.nextNo("mrp_bom"));
        fillHead(bom, productId, v, qty, remark);
        bom.setStatus(0);
        bom.setIsDefault(0);
        bomMapper.insert(bom);
        insertItems(bom.getId(), items);
        chatterService.trace(BIZ_TYPE, bom.getId(), "创建 BOM " + bom.getNo(), null);
        return bom.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBom(Long id, Long productId, Integer version, BigDecimal qty, String remark, List<Item> items) {
        ErpMrpBomDO bom = getBom(id);
        validate(productId, qty, items);
        fillHead(bom, productId, version, qty, remark);
        bomMapper.updateById(bom);
        bomItemMapper.delete(new LambdaQueryWrapper<ErpMrpBomItemDO>()
                .eq(ErpMrpBomItemDO::getBomId, id));
        insertItems(id, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBom(Long id) {
        getBom(id);
        bomMapper.physicalDeleteById(id);
        bomItemMapper.physicalDeleteByBomId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableBom(Long id) {
        ErpMrpBomDO bom = getBom(id);
        // 同产品其他版本取消默认
        bomMapper.update(null, new LambdaUpdateWrapper<ErpMrpBomDO>()
                .eq(ErpMrpBomDO::getProductId, bom.getProductId())
                .set(ErpMrpBomDO::getIsDefault, 0));
        bom.setStatus(STATUS_ENABLED);
        bom.setIsDefault(1);
        bomMapper.updateById(bom);
        chatterService.trace(BIZ_TYPE, id, "启用 BOM " + bom.getNo() + " 并设为默认", null);
    }

    @Override
    public ErpMrpBomDO getDefaultBom(Long productId) {
        return bomMapper.selectOne(new LambdaQueryWrapper<ErpMrpBomDO>()
                .eq(ErpMrpBomDO::getProductId, productId)
                .eq(ErpMrpBomDO::getStatus, STATUS_ENABLED)
                .eq(ErpMrpBomDO::getIsDefault, 1)
                .last("LIMIT 1"));
    }

    @Override
    public List<ErpMrpBomItemDO> expandToLeaves(Long productId, BigDecimal productQty) {
        Map<Long, BigDecimal> leafQty = new HashMap<>();
        expand(productId, productQty, 0, leafQty);
        List<ErpMrpBomItemDO> leaves = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : leafQty.entrySet()) {
            ErpMrpBomItemDO leaf = new ErpMrpBomItemDO();
            leaf.setProductId(entry.getKey());
            leaf.setQty(entry.getValue());
            leaves.add(leaf);
        }
        return leaves;
    }

    /** 递归展开：产品有默认 BOM 则继续下钻，无 BOM 视为叶子 */
    private void expand(Long productId, BigDecimal qty, int depth, Map<Long, BigDecimal> leafQty) {
        if (depth > MAX_DEPTH) {
            throw of(9_00, 1, "BOM 展开超过最大层级（疑似环形引用），产品: " + productId);
        }
        ErpMrpBomDO bom = getDefaultBom(productId);
        if (bom == null || bom.getQty().signum() <= 0) {
            leafQty.merge(productId, qty, BigDecimal::add);
            return;
        }
        for (ErpMrpBomItemDO item : getItemList(bom.getId())) {
            BigDecimal componentQty = item.getQty()
                    .multiply(qty)
                    .divide(bom.getQty(), 6, RoundingMode.HALF_UP);
            BigDecimal lossRate = item.getLossRate() == null ? BigDecimal.ZERO : item.getLossRate();
            BigDecimal withLoss = componentQty.multiply(BigDecimal.ONE.add(lossRate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)));
            expand(item.getProductId(), withLoss, depth + 1, leafQty);
        }
    }

    private void fillHead(ErpMrpBomDO bom, Long productId, Integer version, BigDecimal qty, String remark) {
        bom.setProductId(productId);
        bom.setVersion(version != null ? version : 1);
        bom.setQty(qty);
        bom.setRemark(remark);
    }

    private void insertItems(Long bomId, List<Item> items) {
        for (Item item : items) {
            ErpMrpBomItemDO itemDO = new ErpMrpBomItemDO();
            itemDO.setBomId(bomId);
            itemDO.setProductId(item.productId());
            itemDO.setQty(item.qty());
            itemDO.setLossRate(item.lossRate() == null ? BigDecimal.ZERO : item.lossRate());
            itemDO.setRemark(item.remark());
            bomItemMapper.insert(itemDO);
        }
    }

    private void validate(Long productId, BigDecimal qty, List<Item> items) {
        if (productId == null || qty == null || qty.signum() <= 0) {
            throw of(9_00, 2, "产品与产出基数不能为空且基数须为正数");
        }
        if (items == null || items.isEmpty()) {
            throw of(9_00, 3, "BOM 组件行不能为空");
        }
        for (Item item : items) {
            if (item.productId() == null || item.qty() == null || item.qty().signum() <= 0) {
                throw of(9_00, 4, "组件行存在产品/单耗不合法的行");
            }
        }
    }

    private ErpMrpBomDO getBom(Long id) {
        ErpMrpBomDO bom = bomMapper.selectById(id);
        if (bom == null) {
            throw new ServiceException(9_00_0001, "BOM 不存在");
        }
        return bom;
    }
}

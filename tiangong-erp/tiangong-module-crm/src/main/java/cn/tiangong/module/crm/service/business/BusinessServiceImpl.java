package cn.tiangong.module.crm.service.business;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.api.PartnerApi;
import cn.tiangong.module.crm.dal.dataobject.CrmBusinessDO;
import cn.tiangong.module.crm.dal.mysql.CrmBusinessMapper;
import cn.tiangong.module.sal.api.SaleOrderApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class BusinessServiceImpl implements BusinessService {

    public static final String BIZ_TYPE = "crm_business";
    public static final int STAGE_INIT = 1;
    public static final int STAGE_WIN = 5;
    public static final int STAGE_LOSE = 6;

    private final CrmBusinessMapper businessMapper;
    private final PartnerApi partnerApi;
    private final ChatterService chatterService;
    private final SaleOrderApi saleOrderApi;

    @Override
    public PageResult<CrmBusinessDO> getBusinessPage(PageParam pageParam, String name, Long partnerId, Integer stage) {
        LambdaQueryWrapper<CrmBusinessDO> wrapper = new LambdaQueryWrapper<CrmBusinessDO>()
                .like(name != null && !name.isBlank(), CrmBusinessDO::getName, name)
                .eq(partnerId != null, CrmBusinessDO::getPartnerId, partnerId)
                .eq(stage != null, CrmBusinessDO::getStage, stage)
                .orderByDesc(CrmBusinessDO::getId);
        return businessMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Map<Integer, long[]> stageStatistics() {
        Map<Integer, long[]> result = new HashMap<>();
        for (CrmBusinessDO b : businessMapper.selectList(null)) {
            long[] stat = result.computeIfAbsent(b.getStage(), k -> new long[2]);
            stat[0] += 1;
            stat[1] += b.getAmount() == null ? 0 : b.getAmount().longValue();
        }
        return result;
    }

    @Override
    public Long createBusiness(String name, Long partnerId, BigDecimal amount, Integer stage,
                               LocalDate expectDate, String remark) {
        validate(name, partnerId);
        CrmBusinessDO business = new CrmBusinessDO();
        fill(business, name, partnerId, amount, stage, expectDate, remark);
        business.setOwnerUserId(SecurityFrameworkUtils.getLoginUserId());
        businessMapper.insert(business);
        String partnerName = partnerApi.getPartnerNames(java.util.List.of(partnerId))
                .getOrDefault(partnerId, "");
        chatterService.trace(BIZ_TYPE, business.getId(),
                "创建商机 " + name + "（客户：" + partnerName + "）", null);
        return business.getId();
    }

    @Override
    public void updateBusiness(Long id, String name, Long partnerId, BigDecimal amount,
                               LocalDate expectDate, String remark) {
        CrmBusinessDO business = getBusiness(id);
        fill(business, name, partnerId, amount, business.getStage(), expectDate, remark);
        businessMapper.updateById(business);
    }

    @Override
    public void updateStage(Long id, Integer stage) {
        CrmBusinessDO business = getBusiness(id);
        if (stage == null || stage < 1 || stage > 6) {
            throw of(10_01, 1, "阶段取值非法");
        }
        if (business.getStage() != null && (business.getStage() == STAGE_WIN || business.getStage() == STAGE_LOSE)) {
            throw of(10_01, 2, "赢单/输单为终态，不可再变更");
        }
        business.setStage(stage);
        businessMapper.updateById(business);
        chatterService.trace(BIZ_TYPE, id, "商机阶段推进至：" + stageLabel(stage), null);
    }

    @Override
    public Long toSaleOrder(Long id, Long productId, BigDecimal qty, BigDecimal price) {
        CrmBusinessDO business = getBusiness(id);
        if (business.getStage() != STAGE_WIN) {
            throw of(10_01, 4, "只有赢单商机才能转销售订单");
        }
        if (productId == null || qty == null || qty.signum() <= 0) {
            throw of(10_01, 5, "请选择产品与数量");
        }
        Long orderId = saleOrderApi.createDraftOrder(business.getPartnerId(),
                "商机转单 " + business.getName(),
                List.of(new SaleOrderApi.Item(productId, null, qty,
                        price == null ? business.getAmount() : price, null)));
        chatterService.trace(BIZ_TYPE, id, "赢单商机已转销售订单（订单 id " + orderId + "）", null);
        return orderId;
    }

    @Override
    public void deleteBusiness(Long id) {
        getBusiness(id);
        businessMapper.deleteById(id);
    }

    private String stageLabel(Integer stage) {
        return switch (stage) {
            case 1 -> "初步接触";
            case 2 -> "需求确认";
            case 3 -> "方案报价";
            case 4 -> "商务谈判";
            case 5 -> "赢单";
            case 6 -> "输单";
            default -> String.valueOf(stage);
        };
    }

    private void validate(String name, Long partnerId) {
        if (name == null || name.isBlank() || partnerId == null) {
            throw of(10_01, 2, "商机名称与客户不能为空");
        }
    }

    private void fill(CrmBusinessDO business, String name, Long partnerId, BigDecimal amount,
                      Integer stage, LocalDate expectDate, String remark) {
        business.setName(name != null ? name : business.getName());
        business.setPartnerId(partnerId != null ? partnerId : business.getPartnerId());
        business.setAmount(amount);
        business.setStage(stage != null ? stage : (business.getStage() == null ? STAGE_INIT : business.getStage()));
        business.setExpectDate(expectDate);
        business.setRemark(remark);
    }

    private CrmBusinessDO getBusiness(Long id) {
        CrmBusinessDO business = businessMapper.selectById(id);
        if (business == null) {
            throw of(10_01, 3, "商机不存在");
        }
        return business;
    }
}

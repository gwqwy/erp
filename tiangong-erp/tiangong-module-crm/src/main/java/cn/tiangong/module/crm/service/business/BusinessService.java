package cn.tiangong.module.crm.service.business;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.crm.dal.dataobject.CrmBusinessDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface BusinessService {

    PageResult<CrmBusinessDO> getBusinessPage(PageParam pageParam, String name, Long partnerId, Integer stage);

    /** 漏斗统计：各阶段商机数量与金额 */
    Map<Integer, long[]> stageStatistics();

    Long createBusiness(String name, Long partnerId, BigDecimal amount, Integer stage,
                        LocalDate expectDate, String remark);

    void updateBusiness(Long id, String name, Long partnerId, BigDecimal amount,
                        LocalDate expectDate, String remark);

    /** 推进/回退漏斗阶段（赢单 5 / 输单 6 为终态，不可再变更） */
    void updateStage(Long id, Integer stage);

    /** 赢单商机一键转销售订单草稿，返回订单编号 */
    Long toSaleOrder(Long id, Long productId, java.math.BigDecimal qty, java.math.BigDecimal price);

    void deleteBusiness(Long id);
}

package cn.tiangong.module.crm.service.lead;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.crm.dal.dataobject.CrmLeadDO;

public interface LeadService {

    PageResult<CrmLeadDO> getLeadPage(PageParam pageParam, String name, Integer status);

    Long createLead(String name, String source, String contact, String phone, String remark);

    void updateLead(Long id, String name, String source, String contact, String phone, String remark);

    void deleteLead(Long id);

    /** 线索转商机：线索状态置为已转商机，返回商机编号 */
    Long convertToBusiness(Long id, Long partnerId, java.math.BigDecimal amount,
                           java.time.LocalDate expectDate);
}

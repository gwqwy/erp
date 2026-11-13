package cn.tiangong.module.crm.service.lead;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.crm.dal.dataobject.CrmBusinessDO;
import cn.tiangong.module.crm.dal.dataobject.CrmLeadDO;
import cn.tiangong.module.crm.dal.mysql.CrmBusinessMapper;
import cn.tiangong.module.crm.dal.mysql.CrmLeadMapper;
import cn.tiangong.module.crm.service.business.BusinessServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class LeadServiceImpl implements LeadService {

    public static final String BIZ_TYPE = "crm_lead";
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_CONVERTED = 2;
    public static final int STATUS_INVALID = 3;

    private final CrmLeadMapper leadMapper;
    private final CrmBusinessMapper businessMapper;
    private final ChatterService chatterService;

    @Override
    public PageResult<CrmLeadDO> getLeadPage(PageParam pageParam, String name, Integer status) {
        LambdaQueryWrapper<CrmLeadDO> wrapper = new LambdaQueryWrapper<CrmLeadDO>()
                .like(name != null && !name.isBlank(), CrmLeadDO::getName, name)
                .eq(status != null, CrmLeadDO::getStatus, status)
                .orderByDesc(CrmLeadDO::getId);
        return leadMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Long createLead(String name, String source, String contact, String phone, String remark) {
        if (name == null || name.isBlank()) {
            throw of(10_00, 1, "线索名称不能为空");
        }
        CrmLeadDO lead = new CrmLeadDO();
        fill(lead, name, source, contact, phone, remark);
        lead.setStatus(STATUS_PENDING);
        lead.setOwnerUserId(SecurityFrameworkUtils.getLoginUserId());
        leadMapper.insert(lead);
        chatterService.trace(BIZ_TYPE, lead.getId(), "创建线索 " + name, null);
        return lead.getId();
    }

    @Override
    public void updateLead(Long id, String name, String source, String contact, String phone, String remark) {
        CrmLeadDO lead = getLead(id);
        fill(lead, name, source, contact, phone, remark);
        leadMapper.updateById(lead);
    }

    @Override
    public void deleteLead(Long id) {
        getLead(id);
        leadMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToBusiness(Long id, Long partnerId, BigDecimal amount, LocalDate expectDate) {
        CrmLeadDO lead = getLead(id);
        if (partnerId == null) {
            throw of(10_00, 2, "请选择关联客户");
        }
        if (lead.getStatus() == STATUS_CONVERTED) {
            throw of(10_00, 3, "该线索已转过商机");
        }
        CrmBusinessDO business = new CrmBusinessDO();
        business.setName(lead.getName());
        business.setPartnerId(partnerId);
        business.setAmount(amount);
        business.setStage(BusinessServiceImpl.STAGE_INIT);
        business.setExpectDate(expectDate);
        business.setSourceLeadId(lead.getId());
        business.setOwnerUserId(SecurityFrameworkUtils.getLoginUserId());
        business.setRemark("由线索转入：" + lead.getName());
        businessMapper.insert(business);

        lead.setStatus(STATUS_CONVERTED);
        leadMapper.updateById(lead);
        chatterService.trace(BIZ_TYPE, lead.getId(), "线索转为商机 " + business.getName(), null);
        return business.getId();
    }

    private void fill(CrmLeadDO lead, String name, String source, String contact, String phone, String remark) {
        lead.setName(name != null ? name : lead.getName());
        lead.setSource(source);
        lead.setContact(contact);
        lead.setPhone(phone);
        lead.setRemark(remark);
    }

    private CrmLeadDO getLead(Long id) {
        CrmLeadDO lead = leadMapper.selectById(id);
        if (lead == null) {
            throw of(10_00, 4, "线索不存在");
        }
        return lead;
    }
}

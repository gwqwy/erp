package cn.tiangong.module.bas.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.dal.dataobject.ErpPartnerDO;
import cn.tiangong.module.bas.dal.mysql.ErpPartnerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class PartnerServiceImpl implements PartnerService {

    private final ErpPartnerMapper partnerMapper;
    private final SerialNoService serialNoService;

    @Override
    public PageResult<ErpPartnerDO> getPartnerPage(PageParam pageParam, String name, Boolean isCustomer,
                                                   Boolean isSupplier, Integer status) {
        LambdaQueryWrapper<ErpPartnerDO> wrapper = new LambdaQueryWrapper<ErpPartnerDO>()
                .like(name != null && !name.isBlank(), ErpPartnerDO::getName, name)
                .eq(isCustomer != null, ErpPartnerDO::getIsCustomer, isCustomer)
                .eq(isSupplier != null, ErpPartnerDO::getIsSupplier, isSupplier)
                .eq(status != null, ErpPartnerDO::getStatus, status)
                .orderByDesc(ErpPartnerDO::getId);
        return partnerMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public ErpPartnerDO getPartner(Long id) {
        ErpPartnerDO partner = partnerMapper.selectById(id);
        if (partner == null) {
            throw of(2_004, 1, "往来单位不存在");
        }
        return partner;
    }

    @Override
    public Long createPartner(ErpPartnerDO partner) {
        if (partner.getNo() == null || partner.getNo().isBlank()) {
            partner.setNo(serialNoService.nextNo("erp_partner"));
        }
        if (partner.getStatus() == null) {
            partner.setStatus(0);
        }
        partnerMapper.insert(partner);
        return partner.getId();
    }

    @Override
    public void updatePartner(ErpPartnerDO partner) {
        getPartner(partner.getId());
        partner.setNo(null);
        partnerMapper.updateById(partner);
    }

    @Override
    public void deletePartner(Long id) {
        getPartner(id);
        partnerMapper.deleteById(id);
    }

    @Override
    public void updatePartnerStatus(Long id, Integer status) {
        ErpPartnerDO partner = getPartner(id);
        partner.setStatus(status);
        partnerMapper.updateById(partner);
    }
}

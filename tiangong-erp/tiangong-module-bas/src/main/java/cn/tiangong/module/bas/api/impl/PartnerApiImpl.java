package cn.tiangong.module.bas.api.impl;

import cn.tiangong.module.bas.dal.dataobject.ErpPartnerDO;
import cn.tiangong.module.bas.dal.mysql.ErpPartnerMapper;
import cn.tiangong.module.bas.api.PartnerApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PartnerApiImpl implements PartnerApi {

    private final ErpPartnerMapper partnerMapper;

    @Override
    public Map<Long, String> getPartnerNames(Collection<Long> partnerIds) {
        if (partnerIds == null || partnerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return partnerMapper.selectBatchIds(partnerIds).stream()
                .collect(Collectors.toMap(ErpPartnerDO::getId, ErpPartnerDO::getName, (a, b) -> a));
    }

    @Override
    public BigDecimal getCreditLimit(Long partnerId) {
        ErpPartnerDO partner = partnerMapper.selectById(partnerId);
        return partner == null ? null : partner.getCreditLimit();
    }
}

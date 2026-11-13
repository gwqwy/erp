package cn.tiangong.module.pur.api.impl;

import cn.tiangong.module.pur.api.PurStatApi;
import cn.tiangong.module.pur.dal.dataobject.ErpPurchaseInboundDO;
import cn.tiangong.module.pur.dal.mysql.ErpPurchaseInboundMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class PurStatApiImpl implements PurStatApi {

    private final ErpPurchaseInboundMapper inboundMapper;

    @Override
    public List<Map<String, Object>> monthlyPurchaseAmount(int months) {
        QueryWrapper<ErpPurchaseInboundDO> wrapper = new QueryWrapper<ErpPurchaseInboundDO>()
                .select("DATE_FORMAT(inbound_date, '%Y-%m') AS ym", "IFNULL(SUM(total_amount), 0) AS amt")
                .eq("status", 30)
                .apply("inbound_date >= DATE_SUB(CURDATE(), INTERVAL {0} MONTH)", months)
                .groupBy("ym")
                .orderByAsc("ym");
        return inboundMapper.selectMaps(wrapper);
    }
}

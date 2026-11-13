package cn.tiangong.module.sal.api.impl;

import cn.tiangong.module.sal.api.SalStatApi;
import cn.tiangong.module.sal.dal.dataobject.ErpSaleOutboundDO;
import cn.tiangong.module.sal.dal.mysql.ErpSaleOutboundMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class SalStatApiImpl implements SalStatApi {

    private final ErpSaleOutboundMapper outboundMapper;

    @Override
    public List<Map<String, Object>> monthlySalesAmount(int months) {
        QueryWrapper<ErpSaleOutboundDO> wrapper = new QueryWrapper<ErpSaleOutboundDO>()
                .select("DATE_FORMAT(outbound_date, '%Y-%m') AS ym", "IFNULL(SUM(total_amount), 0) AS amt")
                .eq("status", 30)
                .apply("outbound_date >= DATE_SUB(CURDATE(), INTERVAL {0} MONTH)", months)
                .groupBy("ym")
                .orderByAsc("ym");
        return outboundMapper.selectMaps(wrapper);
    }
}

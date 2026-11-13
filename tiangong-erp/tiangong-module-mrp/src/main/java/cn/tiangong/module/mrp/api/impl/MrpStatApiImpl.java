package cn.tiangong.module.mrp.api.impl;

import cn.tiangong.module.mrp.api.MrpStatApi;
import cn.tiangong.module.mrp.dal.dataobject.ErpMrpWorkOrderDO;
import cn.tiangong.module.mrp.dal.mysql.ErpMrpWorkOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MrpStatApiImpl implements MrpStatApi {

    private final ErpMrpWorkOrderMapper workOrderMapper;

    @Override
    public long activeWorkOrders() {
        return workOrderMapper.selectCount(new LambdaQueryWrapper<ErpMrpWorkOrderDO>()
                .in(ErpMrpWorkOrderDO::getStatus, List.of(30, 40)));
    }
}

package cn.tiangong.module.bas.api.impl;

import cn.tiangong.module.bas.api.SerialNoApi;
import cn.tiangong.module.bas.service.SerialNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SerialNoApiImpl implements SerialNoApi {

    private final SerialNoService serialNoService;

    @Override
    public String nextNo(String bizType) {
        return serialNoService.nextNo(bizType);
    }

    @Override
    public String previewNo(String bizType) {
        return serialNoService.previewNo(bizType);
    }
}

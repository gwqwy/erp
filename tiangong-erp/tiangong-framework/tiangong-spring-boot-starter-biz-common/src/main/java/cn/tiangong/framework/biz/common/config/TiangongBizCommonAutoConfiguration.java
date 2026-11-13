package cn.tiangong.framework.biz.common.config;

import cn.tiangong.framework.biz.common.chatter.service.ChatterService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class TiangongBizCommonAutoConfiguration {

    @Bean
    public ChatterService chatterService(cn.tiangong.framework.biz.common.chatter.dal.mysql.BizChatterMapper chatterMapper) {
        return new ChatterService(chatterMapper);
    }
}

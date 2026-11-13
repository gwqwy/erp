package cn.tiangong.framework.web.config;

import cn.tiangong.framework.web.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Web 自动配置：
 * 1. 注册全局异常处理器；
 * 2. Jackson 将 Long/long 序列化为 String——前端 JS Number 精度只有 2^53，
 *    后端雪花 ID 超出后会产生精度丢失，这是 MyBatis Plus ASSIGN_ID 必配项。
 */
@AutoConfiguration
public class TiangongWebAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}

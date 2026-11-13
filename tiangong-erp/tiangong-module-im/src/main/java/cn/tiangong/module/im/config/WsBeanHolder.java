package cn.tiangong.module.im.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * WebSocket 端点实例由 WS 容器自行 new（不经过 Spring），
 * 通过本静态持有者取容器 Bean（Boot 内嵌 Tomcat 无 root context，不能用 SpringConfigurator）。
 */
@Component
public class WsBeanHolder implements ApplicationContextAware {

    private static ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }

    public static <T> T get(Class<T> clazz) {
        return ctx.getBean(clazz);
    }
}

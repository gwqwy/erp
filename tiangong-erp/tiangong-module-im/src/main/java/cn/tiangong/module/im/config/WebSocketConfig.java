package cn.tiangong.module.im.config;

import cn.tiangong.module.im.websocket.ImWebSocketEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置：注册 @ServerEndpoint 端点（外置 Tomcat 必须显式注册 Exporter）。
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

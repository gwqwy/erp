package cn.tiangong.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置项。
 */
@Data
@ConfigurationProperties("tiangong.security")
public class SecurityProperties {

    /** JWT 签名密钥，长度必须 >= 32 字符，生产环境务必通过环境变量覆盖 */
    private String jwtSecret = "tiangong-erp-dev-secret-key-change-me-in-production";

    /** Token 有效期，默认 8 小时 */
    private Duration tokenExpire = Duration.ofHours(8);

    /** 请求头名称 */
    private String tokenHeader = "Authorization";

    /** 额外放行的路径（Ant 风格） */
    private List<String> permitAllUrls = new ArrayList<>();
}

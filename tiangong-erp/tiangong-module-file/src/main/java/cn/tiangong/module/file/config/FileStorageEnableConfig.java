package cn.tiangong.module.file.config;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 x-file-storage（该组件通过 @EnableFileStorage 导入自动配置，
 * 平台参数读 dromara.x-file-storage.* 配置，见 application.yaml）。
 */
@Configuration
@EnableFileStorage
public class FileStorageEnableConfig {
}

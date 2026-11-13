package cn.tiangong.module.stk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 启用定时任务（库存预警每日扫描）。 */
@Configuration
@EnableScheduling
public class StkSchedulingConfig {
}

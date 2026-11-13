package cn.tiangong.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 天工 ERP 启动类。
 * scanBasePackages 覆盖全部 cn.tiangong 组件（各业务模块的 controller/service/初始化器）；
 * MapperScan 覆盖各业务模块的 dal.mysql 包，替代逐个 @Mapper 注解。
 */
@SpringBootApplication(scanBasePackages = "cn.tiangong")
@MapperScan({"cn.tiangong.module.system.dal.mysql", "cn.tiangong.module.bas.dal.mysql",
        "cn.tiangong.module.stk.dal.mysql", "cn.tiangong.module.pur.dal.mysql",
        "cn.tiangong.module.sal.dal.mysql", "cn.tiangong.module.fin.dal.mysql",
        "cn.tiangong.module.mrp.dal.mysql", "cn.tiangong.module.crm.dal.mysql",
        "cn.tiangong.module.hr.dal.mysql", "cn.tiangong.module.bpm.dal.mysql",
        "cn.tiangong.module.infra.dal.mysql", "cn.tiangong.module.file.dal.mysql",
        "cn.tiangong.module.im.dal.mysql", "cn.tiangong.module.rpt.dal.mysql",
        "cn.tiangong.framework.mybatis.core.mapper",
        "cn.tiangong.framework.biz.common.chatter.dal.mysql"})
public class TiangongServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiangongServerApplication.class, args);
    }
}

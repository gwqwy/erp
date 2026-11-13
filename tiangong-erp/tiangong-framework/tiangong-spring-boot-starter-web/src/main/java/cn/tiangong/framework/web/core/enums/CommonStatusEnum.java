package cn.tiangong.framework.web.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态：全系统统一的启用/停用语义（对应字典 common_status）。
 */
@Getter
@AllArgsConstructor
public enum CommonStatusEnum {

    ENABLE(0, "启用"),
    DISABLE(1, "停用");

    private final Integer value;
    private final String label;
}

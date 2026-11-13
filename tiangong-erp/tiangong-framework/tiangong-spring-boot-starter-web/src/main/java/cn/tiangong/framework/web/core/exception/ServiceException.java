package cn.tiangong.framework.web.core.exception;

import lombok.Getter;

/**
 * 业务异常：直接以错误码 + 可读消息抛出，由全局异常处理器转换为 CommonResult。
 */
@Getter
public class ServiceException extends RuntimeException {

    private final Integer code;

    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /** 快捷方式：模块段 3 位 + 4 位序号，如 error(3_04_0001, "库存不足") */
    public static ServiceException of(int segment, int seq, String message) {
        return new ServiceException(segment * 10_000 + seq, message);
    }
}

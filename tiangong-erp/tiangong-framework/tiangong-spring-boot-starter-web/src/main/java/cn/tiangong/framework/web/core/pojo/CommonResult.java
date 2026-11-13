package cn.tiangong.framework.web.core.pojo;

import cn.tiangong.framework.web.core.exception.ServiceException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用 API 返回体。code = 0 表示成功，非 0 见《04-API与开发规范》错误码体系。
 */
@Data
public class CommonResult<T> implements Serializable {

    private Integer code;

    private T data;

    private String msg;

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.code = 0;
        result.data = data;
        result.msg = "";
        return result;
    }

    public static <T> CommonResult<T> error(Integer code, String msg) {
        CommonResult<T> result = new CommonResult<>();
        result.code = code;
        result.data = null;
        result.msg = msg;
        return result;
    }

    public static <T> CommonResult<T> error(ServiceException ex) {
        return error(ex.getCode(), ex.getMessage());
    }

    @JsonIgnore
    public boolean isSuccess() {
        return code != null && code == 0;
    }
}

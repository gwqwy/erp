package cn.tiangong.framework.web.handler;

import cn.tiangong.framework.web.core.exception.ServiceException;
import cn.tiangong.framework.web.core.pojo.CommonResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.NoSuchElementException;

/**
 * 全局异常处理器：统一转换为 CommonResult。
 * 错误码段位：400 客户端参数错误 / 401 未登录 / 403 无权限 / 404 不存在 / 500 服务端异常。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：错误码由业务方给出 */
    @ExceptionHandler(ServiceException.class)
    public CommonResult<Void> serviceExceptionHandler(ServiceException ex) {
        log.info("[serviceExceptionHandler] 业务异常: code={}, msg={}", ex.getCode(), ex.getMessage());
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    /** Spring Validation @Valid 请求体校验失败 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public CommonResult<Void> validationExceptionHandler(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : "请求参数不正确";
        log.info("[validationExceptionHandler] 参数校验失败: {}", msg);
        return CommonResult.error(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResult<Void> constraintViolationExceptionHandler(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream().findFirst()
                .map(v -> v.getMessage()).orElse("请求参数不正确");
        log.info("[constraintViolationExceptionHandler] 参数校验失败: {}", msg);
        return CommonResult.error(400, msg);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, IllegalStateException.class})
    public CommonResult<Void> badRequestHandler(Exception ex) {
        log.info("[badRequestHandler] 请求参数不正确: {}", ex.getMessage());
        return CommonResult.error(400, "请求参数不正确: " + ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public CommonResult<Void> noSuchElementHandler(NoSuchElementException ex) {
        return CommonResult.error(404, "数据不存在");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public CommonResult<Void> noHandlerFoundHandler(NoHandlerFoundException ex) {
        return CommonResult.error(404, "接口不存在: " + ex.getRequestURL());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public CommonResult<Void> methodNotSupportedHandler(HttpRequestMethodNotSupportedException ex) {
        return CommonResult.error(405, "请求方法不支持: " + ex.getMessage());
    }

    /** 认证/鉴权异常由 security starter 的 SecurityExceptionHandler 处理 */

    /** 兜底：未预期异常 */
    @ExceptionHandler(Exception.class)
    public CommonResult<Void> defaultExceptionHandler(Exception ex) {
        log.error("[defaultExceptionHandler] 未预期异常", ex);
        return CommonResult.error(500, "系统异常");
    }
}

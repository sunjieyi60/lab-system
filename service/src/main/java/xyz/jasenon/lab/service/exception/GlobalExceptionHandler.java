package xyz.jasenon.lab.service.exception;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cn.dev33.satoken.exception.NotLoginException;
import xyz.jasenon.lab.common.utils.R;

import java.util.stream.Collectors;

/**
 * @author Jasenon_ce
 * @date 2025/11/29
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public R handle(IllegalArgumentException e) {
        return R.fail("参数不合法:"+e.getMessage());
    }

    /** JSR 380 校验失败：@RequestBody + @Validated */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail("参数校验失败: " + msg);
    }

    /** @RequestBody 反序列化失败（如 JSON 格式/类型不匹配），常导致 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        return R.fail("请求体解析失败: " + msg);
    }

    /** JSR 380 校验失败：@ModelAttribute + @Validated（如日志查询 DTO） */
    @ExceptionHandler(BindException.class)
    public R handleBindException(BindException e) {
        if (e.getBindingResult().getFieldErrorCount() == 0) {
            return R.fail("参数校验失败");
        }
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail("参数校验失败: " + msg);
    }

    @ExceptionHandler(PermissionsInsufficientException.class)
    public R handler(PermissionsInsufficientException e){
        return R.fail("权限不足:"+e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public R notLogin(cn.dev33.satoken.exception.NotLoginException e){
        return R.fail("未登录或会话失效");
    }

    @ExceptionHandler(Exception.class)
    public R defaultHandler(Exception e){
        return R.fail("服务器异常:"+e.getMessage());
    }

}

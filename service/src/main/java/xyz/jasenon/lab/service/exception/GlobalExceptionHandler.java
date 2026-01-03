package xyz.jasenon.lab.service.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cn.dev33.satoken.exception.NotLoginException;
import xyz.jasenon.lab.common.utils.R;

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

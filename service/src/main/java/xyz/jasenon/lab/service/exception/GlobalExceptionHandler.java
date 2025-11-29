package xyz.jasenon.lab.service.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import xyz.jasenon.lab.common.utils.R;

/**
 * @author Jasenon_ce
 * @date 2025/11/29
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(exception = IllegalArgumentException.class)
    public R handle(IllegalArgumentException e) {
        return R.fail("参数不合法:"+e.getMessage());
    }

    @ExceptionHandler(exception = PermissionsInsufficientException.class)
    public R handler(PermissionsInsufficientException e){
        return R.fail("权限不足:"+e.getMessage());
    }

}

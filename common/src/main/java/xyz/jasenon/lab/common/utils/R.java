package xyz.jasenon.lab.common.utils;

import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import xyz.jasenon.lab.common.exception.BusinessException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一响应包装类 - 继承 借助DiyResponseEntity 以控制 HTTP 响应
 * @author Jasenon_ce
 */
@Getter
public class R<T> implements Serializable {

    private final Integer code;
    private final boolean ok;
    private final T data;
    private final String msg;

    /**
     * 私有构造，强制使用工厂方法
     */
    private R(int httpStatus, Integer businessCode, boolean ok, T data, String msg) {
        this.code = businessCode;
        this.ok = ok;
        this.data = data;
        this.msg = msg;
    }

    // ========== 成功响应 (HTTP 200) ==========

    /**
     * 成功响应 - 无数据
     */
    public static <T> R<T> success() {
        return new R<>(200, 200, true, null, "业务处理成功");
    }

    /**
     * 成功响应 - 带数据
     */
    public static <T> R<T> success(T data) {
        return new R<>(200, 200, true, data, "业务处理成功");
    }

    /**
     * 成功响应 - 带数据和自定义消息
     */
    public static <T> R<T> success(T data, String msg) {
        return new R<>(200, 200, true, data, msg);
    }

    // ========== 失败响应 (HTTP 200，业务失败) ==========

    /**
     * 失败响应 - 仅消息
     */
    public static <T> R<T> fail(String msg) {
        return new R<>(200, 500, false, null, msg);
    }

    /**
     * 失败响应 - 带状态码和消息
     */
    public static <T> R<T> fail(Integer code, String msg) {
        return new R<>(200, code, false, null, msg);
    }

    /**
     * 失败响应 - 带状态码、消息和数据
     */
    public static <T> R<T> fail(Integer code, String msg, T data) {
        return new R<>(200, code, false, data, msg);
    }

    // ========== HTTP 状态码响应（可选） ==========

    /**
     * 参数错误 - HTTP 400
     */
    public static <T> R<T> badRequest(String msg) {
        return new R<>(400, 400, false, null, msg);
    }

    /**
     * 未授权 - HTTP 401
     */
    public static <T> R<T> unauthorized(String msg) {
        return new R<>(401, 401, false, null, msg);
    }

    /**
     * 禁止访问 - HTTP 403
     */
    public static <T> R<T> forbidden(String msg) {
        return new R<>(403, 403, false, null, msg);
    }

    /**
     * 资源不存在 - HTTP 404
     */
    public static <T> R<T> notFound(String msg) {
        return new R<>(404, 404, false, null, msg);
    }

    /**
     * 服务器错误 - HTTP 500
     */
    public static <T> R<T> serverError(String msg) {
        return new R<>(500, 500, false, null, msg);
    }

    /**
     * 自定义 HTTP Status 和 业务 Code
     */
    public static <T> R<T> fail(int httpStatus, int businessCode, String msg) {
        return new R<>(httpStatus, businessCode, false, null, msg);
    }

    public static <T> R<T> fail(int httpStatus, int businessCode, String msg, T data) {
        return new R<>(httpStatus, businessCode, false, data, msg);
    }

    // ========== 异常转换 ==========

    /**
     * 转换为业务异常抛出
     * 用于在业务流程中将错误响应直接转为异常，交给全局异常处理器处理
     */
    public BusinessException convert() {
        return new BusinessException(this);
    }
}

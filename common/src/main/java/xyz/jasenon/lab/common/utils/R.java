package xyz.jasenon.lab.common.utils;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author Jasenon_ce
 * @date 2025/11/25
 */
@Getter
@Setter
public class R<T> implements Serializable {
    /**
     * 是否请求成功
     */
    private boolean ok;
    /**
     * 结果码
     */
    private Integer code;
    /**
     * 提示消息
     */
    private String msg = "业务处理成功";
    /**
     * 数据
     */
    private T data;

    public static <T> R<T> success() {
        R<T> r = new R<>();
        r.setOk(true);
        return r;
    }

    public static <T> R<T> success(T data) {
        R<T> r = new R<>();
        r.setOk(true);
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.setOk(false);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.setOk(false);
        r.setMsg(msg);
        r.setCode(code);
        return r;
    }

    public static <T> R<T> fail(Integer code, String msg, T data) {
        R<T> r = new R<>();
        r.setOk(false);
        r.setMsg(msg);
        r.setCode(code);
        r.setData(data);
        return r;
    }

    public static <T> R<T> success(T data,String msg) {
        R<T> r = new R<>();
        r.setOk(true);
        r.setData(data);
        r.setMsg(msg);
        return r;
    }
}

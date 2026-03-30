package xyz.jasenon.lab.common.exception;

import lombok.Getter;
import xyz.jasenon.lab.common.utils.R;

/**
 * 业务异常 - 用于包装 R 响应为异常抛出
 * @author Jasenon_ce
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final boolean ok;
    private final Object data;

    /**
     * 从 R 构造业务异常
     */
    public <T> BusinessException(R<T> r) {
        super(r.getMsg());
        this.code = r.getCode();
        this.ok = r.isOk();
        this.data = r.getData();
    }

    /**
     * 构造业务异常
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.ok = false;
        this.data = null;
    }

    /**
     * 构造业务异常（带数据）
     */
    public BusinessException(Integer code, String message, Object data) {
        super(message);
        this.code = code;
        this.ok = false;
        this.data = data;
    }
}

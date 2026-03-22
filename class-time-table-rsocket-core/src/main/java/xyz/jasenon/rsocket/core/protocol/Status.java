package xyz.jasenon.rsocket.core.protocol;

import lombok.Getter;

/**
 * 自有协议状态码枚举
 * 
 * 状态码规则：
 * - 10000: 成功
 * - 10001-19999: 系统级错误
 * - 20001-29999: 业务级错误
 * - 30001-39999: 设备相关错误
 * - 40001-49999: 网络/通信错误
 */
@Getter
public enum Status implements IStatus {

    // ==================== 成功 ====================
    
    /** 操作成功 */
    C10000(10000, "success", "操作成功"),
    
    // ==================== 系统级错误 (10001-19999) ====================
    
    /** 系统内部错误 */
    C10001(10001, "system_error", "系统内部错误"),
    /** 服务不可用 */
    C10002(10002, "service_unavailable", "服务暂时不可用"),
    /** 配置错误 */
    C10003(10003, "config_error", "配置错误"),
    /** 数据库错误 */
    C10004(10004, "database_error", "数据库操作失败"),
    
    // ==================== 参数/请求错误 (11001-11999) ====================
    
    /** 参数错误 */
    C11001(11001, "param_error", "请求参数错误"),
    /** 参数缺失 */
    C11002(11002, "param_missing", "缺少必要参数"),
    /** 参数格式错误 */
    C11003(11003, "param_format_error", "参数格式错误"),
    /** 消息格式错误 */
    C11004(11004, "invalid_message", "消息格式错误"),
    
    // ==================== 业务级错误 (20001-29999) ====================
    
    /** 业务处理失败 */
    C20001(20001, "business_error", "业务处理失败"),
    /** 操作不允许 */
    C20002(20002, "operation_not_allowed", "当前操作不允许"),
    /** 资源已存在 */
    C20003(20003, "resource_exists", "资源已存在"),
    /** 资源不存在 */
    C20004(20004, "resource_not_found", "资源不存在"),
    
    // ==================== 设备相关错误 (30001-39999) ====================
    
    /** 设备不在线 */
    C30001(30001, "device_offline", "设备不在线"),
    /** 设备未注册 */
    C30002(30002, "device_not_registered", "设备未注册"),
    /** 设备已注册 */
    C30003(30003, "device_already_registered", "设备已注册"),
    /** 设备响应超时 */
    C30004(30004, "device_timeout", "设备响应超时"),
    /** 命令执行失败 */
    C30005(30005, "command_failed", "命令执行失败"),
    /** 不支持的命令 */
    C30006(30006, "unsupported_command", "不支持的命令"),
    /** 设备忙 */
    C30007(30007, "device_busy", "设备忙，请稍后再试"),
    
    // ==================== 网络/通信错误 (40001-49999) ====================
    
    /** 网络错误 */
    C40001(40001, "network_error", "网络连接错误"),
    /** 连接超时 */
    C40002(40002, "connection_timeout", "连接超时"),
    /** 连接断开 */
    C40003(40003, "connection_closed", "连接已断开"),
    /** 发送失败 */
    C40004(40004, "send_failed", "消息发送失败");

    private final Integer code;
    private final String desc;
    private final String msg;

    Status(Integer code, String desc, String msg) {
        this.code = code;
        this.desc = desc;
        this.msg = msg;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    /**
     * 获取描述（用于日志等）
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 判断是否成功状态（code = 10000）
     */
    public boolean isSuccess() {
        return code.equals(C10000.getCode());
    }

    /**
     * 判断是否错误状态
     */
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * 根据 code 查找 Status
     */
    public static Status of(Integer code) {
        if (code == null) {
            return null;
        }
        for (Status status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据 code 查找 Status，找不到返回默认状态
     */
    public static Status ofDefault(Integer code, Status defaultStatus) {
        Status status = of(code);
        return status != null ? status : defaultStatus;
    }
}

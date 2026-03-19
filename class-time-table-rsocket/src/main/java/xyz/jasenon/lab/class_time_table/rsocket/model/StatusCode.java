package xyz.jasenon.lab.class_time_table.rsocket.model;

import lombok.Getter;

/**
 * 状态码枚举
 * 统一定义系统返回状态码
 */
@Getter
public enum StatusCode {
    
    // ==================== 成功状态 (2xx) ====================
    SUCCESS(0, "成功"),
    ACCEPTED(1, "已接受"),
    
    // ==================== 客户端错误 (4xx) ====================
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    DEVICE_OFFLINE(410, "设备离线"),
    FACE_NOT_FOUND(420, "人脸未找到"),
    INVALID_FACE_DATA(421, "人脸数据无效"),
    TIME_TABLE_EMPTY(430, "当前无课表"),
    
    // ==================== 服务端错误 (5xx) ====================
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    DATABASE_ERROR(510, "数据库错误"),
    CACHE_ERROR(520, "缓存错误"),
    
    // ==================== 网络错误 (6xx) ====================
    NETWORK_ERROR(600, "网络错误"),
    CONNECTION_TIMEOUT(601, "连接超时"),
    RESPONSE_TIMEOUT(602, "响应超时"),
    CONNECTION_LOST(603, "连接断开"),
    
    // ==================== 业务错误 (7xx) ====================
    DEVICE_BUSY(700, "设备忙碌"),
    DEVICE_ERROR(701, "设备错误"),
    FACE_DUPLICATE(710, "人脸重复"),
    FACE_REGISTER_FAILED(711, "人脸注册失败"),
    CONFIG_APPLY_FAILED(720, "配置应用失败"),
    DOOR_OPEN_FAILED(730, "开门失败"),
    REBOOT_FAILED(731, "重启失败");
    
    private final int code;
    private final String message;
    
    StatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code >= 0 && this.code < 400;
    }
    
    /**
     * 判断是否客户端错误
     */
    public boolean isClientError() {
        return this.code >= 400 && this.code < 500;
    }
    
    /**
     * 判断是否服务端错误
     */
    public boolean isServerError() {
        return this.code >= 500 && this.code < 600;
    }
    
    /**
     * 判断是否网络错误
     */
    public boolean isNetworkError() {
        return this.code >= 600 && this.code < 700;
    }
    
    /**
     * 判断是否业务错误
     */
    public boolean isBusinessError() {
        return this.code >= 700;
    }
    
    /**
     * 根据 code 获取枚举
     */
    public static StatusCode fromCode(int code) {
        for (StatusCode status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return INTERNAL_ERROR;
    }
    
    /**
     * 快速创建成功状态
     */
    public static StatusCode ok() {
        return SUCCESS;
    }
    
    /**
     * 快速创建错误状态
     */
    public static StatusCode error(String message) {
        return INTERNAL_ERROR;
    }
}

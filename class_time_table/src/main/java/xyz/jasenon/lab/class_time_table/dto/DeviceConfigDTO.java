package xyz.jasenon.lab.class_time_table.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备配置 DTO，与 Android AppConfig 结构对齐
 * REGISTER_ACK 加密后下发给客户端，客户端解密后直接反序列化为 AppConfig 写入 config.json
 *
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConfigDTO {

    /**
     * 设备 ID（注册成功后由服务端分配或回填，客户端写入 config.json）
     */
    @JSONField(name = "device_id")
    private String deviceId;

    /**
     * 后端服务器配置（对应 Android backend_server）
     */
    @JSONField(name = "backend_server")
    private BackendServerConfig backendServer;

    /**
     * 天气配置（对应 Android weather_config）
     */
    @JSONField(name = "weather_config")
    private WeatherConfig weatherConfig;

    /**
     * Hezi 配置：轮询周期、id、key（对应 Android hezi_config）
     */
    @JSONField(name = "hezi_config")
    private HeziConfig heziConfig;

    /**
     * 开门配置（对应 Android door_open_config）
     */
    @JSONField(name = "door_open_config")
    private DoorOpenConfig doorOpenConfig;

    // ---------- 内嵌 DTO，与 Android 一致 ----------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackendServerConfig {
        @JSONField(name = "host")
        private String host;
        @JSONField(name = "port")
        private Integer port;
        @JSONField(name = "timeout")
        private Long timeout;
        @JSONField(name = "heart")
        private Long heartPeriod;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherConfig {
        @JSONField(name = "location")
        private Location location;
        @JSONField(name = "update_interval")
        private Integer updateInterval;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        @JSONField(name = "longitude")
        private Double longitude;
        @JSONField(name = "latitude")
        private Double latitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeziConfig {
        @JSONField(name = "poll_interval")
        private Integer pollIntervalMinutes;
        @JSONField(name = "open_id")
        private String openId;
        @JSONField(name = "app_key")
        private String appKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoorOpenConfig {
        @JSONField(name = "pwd_open_config")
        private PasswordOpenConfig pwdOpenConfig;
        @JSONField(name = "face_open_config")
        private FaceOpenConfig faceOpenConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordOpenConfig {
        @JSONField(name = "password")
        private String password;
        @JSONField(name = "keep_time")
        private KeepTimeConfig keepTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaceOpenConfig {
        @JSONField(name = "precision")
        private Float precision;
        @JSONField(name = "keep_time")
        private KeepTimeConfig keepTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeepTimeConfig {
        @JSONField(name = "keep_time")
        private Integer keepTime;
    }
}

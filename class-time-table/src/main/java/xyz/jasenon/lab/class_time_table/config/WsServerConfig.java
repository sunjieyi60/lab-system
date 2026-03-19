package xyz.jasenon.lab.class_time_table.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import xyz.jasenon.lab.core.packets.Config;

import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties("t-io")
@Data
public class WsServerConfig {
    /**
     * 服务器相关
     */
    public Server server;

    /**
     * 客户端默认配置
     */
    public Config config;

    @Data
    public static class Server {
        /**
         * 服务器名称
         */
        private String name;
        /**
         * 服务器ip
         */
        private String ip;
        /**
         * 服务器port
         */
        private String port;
        /**
         * 设备心跳间隔 单位ms
         */
        private Integer heartbeat;

        private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    }

}

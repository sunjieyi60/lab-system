package xyz.jasenon.lab.class_time_table.t_io.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * t-io 服务器配置属性
 * 
 * @author Jasenon_ce
 */
@Component
@ConfigurationProperties(prefix = "t-io.server")
@Getter
@Setter
public class TioServerProperties {
    
    /**
     * 服务器名称
     */
    private String name = "smart-board-server";
    
    /**
     * 服务器地址
     */
    private String host = "0.0.0.0";
    
    /**
     * 服务器端口号
     */
    private Integer port = 9000;
    
    /**
     * 心跳超时时间，单位毫秒
     */
    private Integer heartbeatTimeout = 60000;
}

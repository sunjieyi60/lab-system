package xyz.jasenon.lab.class_time_table.t_io.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tio.server.TioServer;

/**
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Configuration("t-io")
@Getter
@Setter
public class TioServerConfiguration {

    /**
     * 服务器名称
     */
    @Value("${t-io.server.name:lab-system}")
    private String serverName;

    /**
     * 服务器地址
     */
    @Value("${t-io.server.host:localhost}")
    private String host;

    /**
     * 服务器端口号
     */
    @Value("${t-io.server.port:8765}")
    private Integer port;

    /**
     * 心跳超时时间，单位毫秒
     */
    @Value("${t-io.server.heartbeat:6000}")
    private Integer heartbeatTimeout;



}

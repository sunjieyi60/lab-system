package xyz.jasenon.lab.class_time_table.t_io.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;
import xyz.jasenon.lab.class_time_table.t_io.adapter.TioQosAdapter;
import xyz.jasenon.lab.class_time_table.t_io.handler.SmartBoardTioHandler;
import xyz.jasenon.lab.class_time_table.t_io.listener.SmartBoardTioListener;

import java.io.IOException;

/**
 * t-io 服务器配置
 * 
 * @author Jasenon_ce
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TioServerConfig {
    
    private final TioServerProperties properties;
    private final SmartBoardTioHandler handler;
    private final SmartBoardTioListener listener;
    private final TioQosAdapter qosAdapter;
    
    @Bean
    public TioServer tioServer() throws IOException {
        TioServerConfig tioServerConfig = new TioServerConfig(
                properties.getName(), handler, listener
        );
        tioServerConfig.setHeartbeatTimeout(properties.getHeartbeatTimeout());
        
        TioServer tioServer = new TioServer(tioServerConfig);
        
        // 设置 QoS 适配器的 TioConfig
        qosAdapter.setTioConfig(tioServerConfig);
        
        // 配置 QoS 参数
        qosAdapter.configure(5000, 3, 60000);
        
        // 启动服务器
        tioServer.start(properties.getHost(), properties.getPort());
        
        log.info("t-io 服务器启动成功: {}@{}:{}", 
                properties.getName(), properties.getHost(), properties.getPort());
        
        return tioServer;
    }
}

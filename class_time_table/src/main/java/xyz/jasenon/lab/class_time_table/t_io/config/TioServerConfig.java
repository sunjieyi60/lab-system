package xyz.jasenon.lab.class_time_table.t_io.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tio.server.TioServer;
import xyz.jasenon.lab.class_time_table.t_io.handler.SmartBoardTioHandler;
import xyz.jasenon.lab.class_time_table.t_io.listener.SmartBoardTioListener;

import java.io.IOException;

/**
 * @author Jasenon_ce
 * @date 2026/1/31
 */
@Configuration
@RequiredArgsConstructor
public class TioServerConfig {

    private final TioServerConfiguration config;
    private final SmartBoardTioHandler handler;
    private final SmartBoardTioListener listener;

    @Bean
    public TioServer tioServer() throws IOException {
        org.tio.server.TioServerConfig tioServerConfig = new org.tio.server.TioServerConfig(
            config.getServerName(),handler,listener
        );
        tioServerConfig.setHeartbeatTimeout(config.getHeartbeatTimeout());
        TioServer tioServer = new TioServer(tioServerConfig);
        tioServer.start(config.getHost(), config.getPort());
        return tioServer;
    }

}

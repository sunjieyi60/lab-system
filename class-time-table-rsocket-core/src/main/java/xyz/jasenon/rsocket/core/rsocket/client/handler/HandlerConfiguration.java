package xyz.jasenon.rsocket.core.rsocket.client.handler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Handler 自动配置类
 * 
 * 自动注册所有命令处理器
 */
@Configuration
public class HandlerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RegisterHandler registerHandler() {
        return new RegisterHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public HeartbeatHandler heartbeatHandler() {
        return new HeartbeatHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenDoorHandler openDoorHandler() {
        return new OpenDoorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public UpdateConfigHandler updateConfigHandler() {
        return new UpdateConfigHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public UpdateFaceLibraryHandler updateFaceLibraryHandler() {
        return new UpdateFaceLibraryHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public UpdateScheduleHandler updateScheduleHandler() {
        return new UpdateScheduleHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public RebootHandler rebootHandler() {
        return new RebootHandler();
    }
}

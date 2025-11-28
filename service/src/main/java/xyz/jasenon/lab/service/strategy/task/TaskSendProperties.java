package xyz.jasenon.lab.service.strategy.task;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@ConfigurationProperties(prefix = "task.send")
@Configuration
@Getter
@Setter
public class TaskSendProperties {

    /**
     * MQTT任务发送地址
     */
    private String mqttTaskHost;

    /**
     * Socket任务发送地址
     */
    private String socketTaskHost;

}

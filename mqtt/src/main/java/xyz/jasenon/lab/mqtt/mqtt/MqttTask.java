package xyz.jasenon.lab.mqtt.mqtt;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.dto.task.Task;

@Getter
@Setter
@Accessors(fluent = true)
public class MqttTask extends Task {

    /**
     * 发送主题
     */
    private String sendTopic;

    /**
     * 接收主题
     */
    private String acceptTopic;

    /**
     * 负载数据
     */
    private byte[] payload;

    public MqttTask(Task task){
        super(task);
    }

}

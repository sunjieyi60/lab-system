package xyz.jasenon.lab.mqtt.mqtt;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.dto.task.Task;

@Getter
@Setter
@Accessors(chain = true)
public class MqttTask extends Task {

    /**
     * RS485网关ID
     */
    private Long rs485Id;

    /**
     * 负载数据
     */
    private byte[] payload;

    public MqttTask(Task task){
        super(task);
    }

}

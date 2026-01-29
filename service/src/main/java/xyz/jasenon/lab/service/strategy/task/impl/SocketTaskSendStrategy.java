package xyz.jasenon.lab.service.strategy.task.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.command.SendType;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.service.strategy.task.TaskSendFactory;
import xyz.jasenon.lab.service.strategy.task.TaskSendProperties;
import xyz.jasenon.lab.service.strategy.task.TaskSendStrategy;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Component
@Slf4j
public class SocketTaskSendStrategy implements TaskSendStrategy {

    private final String Host;

    public SocketTaskSendStrategy(TaskSendProperties taskSendProperties){
        this.Host = taskSendProperties.getSocketTaskHost();
    }

    @PostConstruct
    public void init(){
        log.info("Socket任务发送初始化完成");
        TaskSendFactory.register(SendType.SOCKET,this);
    }

    @Override
    public void send(Task task) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String body = objectMapper.writeValueAsString(task);
            HttpResponse response = HttpUtil.createPost(Host).body(body).execute();
            response.close();
        }catch (JsonProcessingException e){
            log.error("MQTT发送任务失败,任务:{}",task.getCommandLine());
            throw new RuntimeException(e);
        }
    }
}

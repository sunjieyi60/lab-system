package xyz.jasenon.lab.mqtt.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.mqtt.mqtt.MqttTask;
import xyz.jasenon.lab.mqtt.mqtt.MqttTaskExplainer;
import xyz.jasenon.lab.mqtt.mqtt.TaskProcessorsManage;

import java.time.LocalDateTime;

/**
 * @author Jasenon_ce
 * @date 2025/11/25
 */
@Api("任务")
@RestController
@RequestMapping("/task")
@Slf4j
public class TaskController {

    @Autowired
    private TaskProcessorsManage taskProcessorsManage;
    @Autowired
    private MqttTaskExplainer mqttTaskExplainer;

    @PostMapping("/add")
    @ApiOperation("添加任务")
    public R addTask(@RequestBody Task task){
        log.info("task 送达:{}", LocalDateTime.now());
        MqttTask mqttTask = mqttTaskExplainer.explainTask(task);
        taskProcessorsManage.addTask(mqttTask);
        return R.success("添加任务成功");
    }

}

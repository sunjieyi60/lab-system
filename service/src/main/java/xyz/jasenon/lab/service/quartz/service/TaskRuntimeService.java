package xyz.jasenon.lab.service.quartz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.service.quartz.model.ScheduleConfigRoot;

@Component
@RequiredArgsConstructor
public class TaskRuntimeService {

    public final ConfigLoader configLoader;

    private void runOnce(String scheduleTaskId){
        // 执行一次任务
        // 1. 获取任务配置
        // 2. 根据任务配置执行任务
        // 3. 更新任务状态为已完成
        ScheduleConfigRoot cfg = configLoader.load(scheduleTaskId);
    }

}

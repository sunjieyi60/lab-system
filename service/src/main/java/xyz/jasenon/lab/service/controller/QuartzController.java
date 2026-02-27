package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.log.LogPoint;
import xyz.jasenon.lab.service.quartz.check.Result;
import xyz.jasenon.lab.service.quartz.config.QuartzRegister;
import xyz.jasenon.lab.service.quartz.model.ScheduleConfigRoot;
import xyz.jasenon.lab.service.quartz.model.ScheduleTask;
import xyz.jasenon.lab.service.quartz.service.ConfigLoader;
import xyz.jasenon.lab.service.quartz.service.TaskRuntimeService;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2026/1/10
 */
@Api("定时任务")
@RestController
@RequestMapping("/quartz")
@CrossOrigin("*")
@RequiredArgsConstructor
public class QuartzController {

    private final ConfigLoader configLoader;
    private final QuartzRegister quartzRegister;
    private final TaskRuntimeService taskRuntimeService;

    @PostMapping("/create")
    @ApiOperation("创建定时任务")
    @LogPoint(title = "'报警联动设置'", sqEl = "#scheduleConfigRoot", clazz = ScheduleConfigRoot.class)
    public R createConfig(@RequestBody ScheduleConfigRoot scheduleConfigRoot){
        Result<Boolean> res = configLoader.configCreate(scheduleConfigRoot);
        if (!res.getData()) {
            return R.fail(res.getMessage());
        }
        // 创建成功后注册到Quartz调度器
        try {
            quartzRegister.scheduleTask(scheduleConfigRoot);
        } catch (org.quartz.SchedulerException e) {
            return R.fail("任务创建成功，但注册到调度器失败: " + e.getMessage());
        }
        return R.success("创建成功");
    }

    @DeleteMapping("/delete")
    @ApiOperation("删除定时任务")
    public R deleteTask(@RequestParam("taskId") String taskId) {
        String err = taskRuntimeService.deleteTask(taskId);
        if (err != null) {
            return R.fail(err);
        }
        return R.success("删除成功");
    }

    @PostMapping("/cancel")
    @ApiOperation("取消定时任务")
    public R cancelTask(@RequestParam("taskId") String taskId) {
        String err = taskRuntimeService.cancelTask(taskId);
        if (err != null) {
            return R.fail(err);
        }
        return R.success("取消成功");
    }

    @PostMapping("/enable")
    @ApiOperation("启用定时任务")
    public R enableTask(@RequestParam("taskId") String taskId) {
        String err = taskRuntimeService.enableTask(taskId);
        if (err != null) {
            return R.fail(err);
        }
        return R.success("启用成功");
    }

    @GetMapping("/list")
    @ApiOperation("获取定时任务列表")
    public R<List<ScheduleTask>> getAllScheduleTask(@RequestParam(required = false) Boolean enable) {
        List<ScheduleTask> tasks = taskRuntimeService.getAllScheduleTask();
        if (enable != null) {
            tasks = tasks.stream()
                    .filter(t -> enable.equals(t.getEnable()))
                    .toList();
        }
        return R.success(tasks);
    }
}

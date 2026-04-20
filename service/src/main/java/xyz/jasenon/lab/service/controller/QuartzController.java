package xyz.jasenon.lab.service.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.service.quartz.model.CourseScheduleTaskGenerator;
import xyz.jasenon.lab.service.quartz.service.TaskGeneratorService;
import xyz.jasenon.lab.service.quartz.service.TaskUpdateService;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.entity.device.DeviceType;
import xyz.jasenon.lab.common.utils.DiyResponseEntity;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.log.LogPoint;
import xyz.jasenon.lab.service.quartz.check.Result;
import xyz.jasenon.lab.service.quartz.config.QuartzRegister;
import xyz.jasenon.lab.service.quartz.model.ScheduleConfigRoot;
import xyz.jasenon.lab.service.quartz.model.ScheduleTask;
import xyz.jasenon.lab.service.quartz.service.ConfigLoader;
import xyz.jasenon.lab.service.quartz.service.TaskQueryService;
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
    private final TaskQueryService taskQueryService;
    private final TaskGeneratorService taskGeneratorService;
    private final TaskUpdateService taskUpdateService;

    @PostMapping("/create")
    @ApiOperation("创建定时任务")
    @LogPoint(title = "'报警联动设置'", sqEl = "#scheduleConfigRoot", clazz = ScheduleConfigRoot.class)
    public DiyResponseEntity<R<String>> createConfig(@RequestBody ScheduleConfigRoot scheduleConfigRoot){
        Result<Boolean> res = configLoader.configCreate(scheduleConfigRoot);
        if (!res.getData()) {
            return DiyResponseEntity.of(R.fail(res.getMessage()));
        }
        // 创建成功后注册到Quartz调度器
        try {
            quartzRegister.scheduleTask(scheduleConfigRoot);
        } catch (org.quartz.SchedulerException e) {
            return DiyResponseEntity.of(R.fail("任务创建成功，但注册到调度器失败: " + e.getMessage()));
        }
        return DiyResponseEntity.of(R.success("创建成功"));
    }

    @DeleteMapping("/delete")
    @ApiOperation("删除定时任务")
    public DiyResponseEntity<R<String>> deleteTask(@RequestParam("taskId") String taskId) {
        String err = taskRuntimeService.deleteTask(taskId);
        if (err != null) {
            return DiyResponseEntity.of(R.fail(err));
        }
        return DiyResponseEntity.of(R.success("删除成功"));
    }

    @PostMapping("/cancel")
    @ApiOperation("取消定时任务")
    public DiyResponseEntity<R<String>> cancelTask(@RequestParam("taskId") String taskId) {
        String err = taskRuntimeService.cancelTask(taskId);
        if (err != null) {
            return DiyResponseEntity.of(R.fail(err));
        }
        return DiyResponseEntity.of(R.success("取消成功"));
    }

    @PostMapping("/enable")
    @ApiOperation("启用定时任务")
    public DiyResponseEntity<R<String>> enableTask(@RequestParam("taskId") String taskId) {
        String err = taskRuntimeService.enableTask(taskId);
        if (err != null) {
            return DiyResponseEntity.of(R.fail(err));
        }
        return DiyResponseEntity.of(R.success("启用成功"));
    }

    @GetMapping("/list")
    @ApiOperation("获取定时任务列表")
    public DiyResponseEntity<R<List<ScheduleTask>>> getAllScheduleTask(@RequestParam(required = false) Boolean enable) {
        List<ScheduleTask> tasks = taskRuntimeService.getAllScheduleTask();
        if (enable != null) {
            tasks = tasks.stream()
                    .filter(t -> enable.equals(t.getEnable()))
                    .toList();
        }
        return DiyResponseEntity.of(R.success(tasks));
    }

    @GetMapping("/list-by-lab/batch")
    @ApiOperation("获取自己相关的所有实验室下的定时任务")
    public DiyResponseEntity<R<List<ScheduleConfigRoot>>> batchGetConfigByLaboratoryIds(@RequestParam Long ...laboratoryIds){
        return DiyResponseEntity.of(R.success(taskQueryService.batchQueryByLaboratoryIds(laboratoryIds)));
    }

    /**
     * 高性能实验室任务查询（支持设备/指令筛选）
     */
    @GetMapping("/list-by-lab")
    @ApiOperation(value = "查询实验室定时任务配置",
                  notes = "支持按设备类型、设备ID、指令筛选,分页查询")
    public DiyResponseEntity<R<Page<ScheduleConfigRoot>>> getConfigsByLaboratory(
            @RequestParam("laboratoryId") @Parameter(description = "实验室ID") Long laboratoryId,
            @RequestParam(required = false) @Parameter(description = "启用状态筛选") Boolean enable,
            @RequestParam(required = false) @Parameter(description = "设备类型筛选") DeviceType deviceType,
            @RequestParam(required = false) @Parameter(description = "设备ID筛选") Long deviceId,
            @RequestParam(required = false) @Parameter(description = "指令筛选") CommandLine commandLine,
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int pageNum,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int pageSize) {

        // 参数校验
        if (pageSize > 100) {
            pageSize = 100;
        }

        Page<ScheduleConfigRoot> result = taskQueryService.queryByLaboratory(
            laboratoryId, enable, deviceType, deviceId, commandLine, pageNum, pageSize
        );

        return DiyResponseEntity.of(R.success(result));
    }

    @PostMapping("/generate-from-course-schedule")
    @ApiOperation("根据课表生成定时任务")
    public DiyResponseEntity<R<Boolean>> generateFromCourseSchedule(@Validated @RequestBody CourseScheduleTaskGenerator target) {
        return DiyResponseEntity.of(taskGeneratorService.generateScheduleTask(target));
    }

    @PutMapping("/update")
    @ApiOperation("更新定时任务配置")
    @LogPoint(title = "'报警联动设置-更新'", sqEl = "#root", clazz = ScheduleConfigRoot.class)
    public DiyResponseEntity<R<Boolean>> updateTask(@RequestBody @Validated ScheduleConfigRoot root) {
        R<Boolean> res = taskUpdateService.updateTask(root);
        if (!Boolean.TRUE.equals(res.getData())) {
            return DiyResponseEntity.of(res);
        }
        // 更新成功后重新注册到 Quartz 调度器
        try {
            quartzRegister.scheduleTask(root);
        } catch (org.quartz.SchedulerException e) {
            return DiyResponseEntity.of(R.fail("任务更新成功，但注册到调度器失败: " + e.getMessage()));
        }
        return DiyResponseEntity.of(R.success(true, "更新成功"));
    }

}

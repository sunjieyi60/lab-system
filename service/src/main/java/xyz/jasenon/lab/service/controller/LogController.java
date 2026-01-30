package xyz.jasenon.lab.service.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.entity.log.OperationLog;
import xyz.jasenon.lab.common.entity.log.AlarmLog;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.log.OperationLogQueryDto;
import xyz.jasenon.lab.service.dto.log.AlarmLogQueryDto;
import xyz.jasenon.lab.service.log.LogTaskManager;
import xyz.jasenon.lab.service.service.IOperationLogService;
import xyz.jasenon.lab.service.service.IAlarmLogService;

/**
 * 日志查询接口：操作日志 & 报警日志。
 * 查询条件用 query DTO（Query 绑定），分页用 @RequestParam，风格同 myCreate。
 */
@Api("日志查询")
@RestController
@RequestMapping("/log")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RequiredArgsConstructor
public class LogController {

    private final IOperationLogService operationLogService;
    private final IAlarmLogService alarmLogService;
    private final LogTaskManager logTaskManager;

    @GetMapping("/operation/page")
    @ApiOperation("操作日志分页查询")
    public R<Page<OperationLog>> pageOperationLog(
            @Validated OperationLogQueryDto query,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        Page<OperationLog> page = operationLogService.pageOperationLog(query, pageNum, pageSize);
        return R.success(page);
    }

    @GetMapping("/alarm/page")
    @ApiOperation("报警日志分页查询")
    public R<Page<AlarmLog>> pageAlarmLog(
            @Validated AlarmLogQueryDto query,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        Page<AlarmLog> page = alarmLogService.pageAlarmLog(query, pageNum, pageSize);
        return R.success(page);
    }

    /**
     * 内部使用：写入一条报警日志。
     * 主要给 MQTT 模块等内部系统调用，不建议前端直接使用。
     * 若启用 Sa-Token 全局登录校验，需在拦截器中放行 POST /log/alarm，否则 MQTT 上报会 401。
     */
    @PostMapping("/alarm")
    @ApiOperation("【内部】写入报警日志")
    public R<Void> createAlarmLog(@RequestBody AlarmLog alarmLog) {
        logTaskManager.submitAlarmLog(alarmLog);
        return R.success(null, "报警日志已提交");
    }
}


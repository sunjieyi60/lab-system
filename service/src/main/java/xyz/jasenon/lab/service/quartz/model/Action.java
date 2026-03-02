package xyz.jasenon.lab.service.quartz.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.device.DeviceType;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
@TableName(value = "action", autoResultMap = true)
public class Action {

    private String id;
    /**
     * 设备类型
     */
    @NotNull(message = "设备类型不能为空")
    private DeviceType deviceType;

    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /**
     * 指令
     */
    @NotNull(message = "指令不能为空")
    private CommandLine commandLine;

    /**
     * 参数列表 按照address selfId 操作参数传递 需观察占位符数量
     */
    @NotEmpty(message = "参数列表不能为空")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Integer[] args;

    private String actionGroupId;

    /**
     * 任务ID（便于批量查询后内存分组，减少N+1）
     */
    private String scheduleTaskId;

    public Task convert2Task(){
        Task task = new Task();
        task.setPriority(TaskPriority.AUTOMATIC);
        task.setDeviceType(deviceType);
        task.setDeviceId(deviceId);
        task.setCommandLine(commandLine);
        task.setArgs(args);
        task.setSendThreadName(Thread.currentThread().getName());
        return task;
    }

}

package xyz.jasenon.lab.common.dto.task;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.entity.device.DeviceType;

@Getter
@Setter
@NoArgsConstructor
public class Task implements Comparable<Task> {

    /**
     * 优先级，越小越优先执行
     */
    @NotNull(message = "优先级不能为空")
    private TaskPriority priority;

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
    private Integer[] args;

    /**
     * asc order
     */
    @Override
    public int compareTo(Task o) {
        return this.priority.getPriority() - o.priority.getPriority();
    }

    public Task(Task task){
        this.priority = task.getPriority();
        this.deviceType = task.getDeviceType();
        this.deviceId = task.getDeviceId();
        this.commandLine = task.getCommandLine();
        this.args = task.getArgs();
    }

}

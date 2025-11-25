package xyz.jasenon.lab.common.dto.task;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.dto.command.CommandLine;
import xyz.jasenon.lab.common.entity.device.DeviceType;

@Getter
@Setter
public class Task implements Comparable<Task> {

    /**
     * 优先级，越小越优先执行
     */
    private TaskPriority priority;

    /**
     * 设备类型
     */
    private DeviceType deviceType;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 指令
     */
    private CommandLine commandLine;
    
    /**
     * 参数列表 按照address selfId 操作参数传递 需观察占位符数量
     */
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

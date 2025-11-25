package xyz.jasenon.lab.common.explain;


import java.text.MessageFormat;
import java.util.Arrays;

import cn.hutool.core.lang.Assert;
import xyz.jasenon.lab.common.dto.command.Command;
import xyz.jasenon.lab.common.dto.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.entity.device.Access;
import xyz.jasenon.lab.common.entity.device.AirCondition;
import xyz.jasenon.lab.common.entity.device.CircuitBreak;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.common.entity.device.Light;
import xyz.jasenon.lab.common.entity.device.Sensor;
import xyz.jasenon.lab.common.service.IDeviceService$;
import xyz.jasenon.lab.common.utils.CrcChecker;
import xyz.jasenon.lab.common.utils.SumChecker;

public abstract class TaskExplain<T> {

    final IDeviceService$ deviceService;

    public TaskExplain(IDeviceService$ deviceService) {
        this.deviceService = deviceService;
    }

    public T explainTask(Task task){
        Device device = deviceService.getDeviceByDeviceId(task.getDeviceId());
        Assert.notNull(device, "设备不存在");
        switch (device.getDeviceType()) {
            case AirCondition:{
                return explainTask(task,(AirCondition) device);
            }
            case Light:{
                return explainTask(task,(Light) device);
            }
            case Sensor:{
                return explainTask(task,(Sensor) device);
            }
            case Access:{
                return explainTask(task,(Access) device);
            }
            case CircuitBreak:{
                return explainTask(task,(CircuitBreak) device);
            }
            default:
                throw new IllegalArgumentException("不支持的设备类型");
        }
    }

    public abstract T explainTask(Task task,AirCondition airCondition);
    public abstract T explainTask(Task task,Light light);
    public abstract T explainTask(Task task,Sensor sensor);
    public abstract T explainTask(Task task,Access access);
    public abstract T explainTask(Task task,CircuitBreak circuitBreak);

    protected final byte[] generatePayload(Task task,Device device){
        CommandLine commandLine = task.getCommandLine();
        Command command = commandLine.getCommand();
        Object[] toHexString = Arrays.stream(task.getArgs()).map(arg->{
            return Integer.toHexString(arg);
        }).toArray(Object[]::new);
        String[] $ = MessageFormat.format(command.getCommandLine(), toHexString).split(" ");
        Byte[] originalPayload = Arrays.asList($).stream().map(s ->{
            Integer value = Integer.parseInt(s,16);
            return value.byteValue();
        }).toArray(Byte[]::new);
        return generatePayload(command, originalPayload);
    }

    private final byte[] generatePayload(Command command,Byte[] orginalPayload){
        switch (command.getCheckType()) {
            case SUM: return SumChecker.generatePayload(orginalPayload);
            case CRC16: return CrcChecker.generatePayload(orginalPayload);
            default:
                throw new IllegalArgumentException("不支持的校验类型");
        }
    }
    
}

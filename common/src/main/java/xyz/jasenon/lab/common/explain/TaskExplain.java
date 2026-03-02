package xyz.jasenon.lab.common.explain;


import xyz.jasenon.lab.common.command.Command;
import xyz.jasenon.lab.common.command.CommandLine;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.entity.device.*;
import xyz.jasenon.lab.common.service.IDeviceService$;
import xyz.jasenon.lab.common.utils.CrcChecker;
import xyz.jasenon.lab.common.utils.SumChecker;

import java.text.MessageFormat;
import java.util.Arrays;

public abstract class 
TaskExplain<T> {

    final IDeviceService$ deviceService;

    public TaskExplain(IDeviceService$ deviceService) {
        this.deviceService = deviceService;
    }

    public T explainTask(Task task){
        Device device = task.getDevice();
        return switch (device.getDeviceType()) {
            case AirCondition -> explainTask(task, (AirCondition) device);
            case Light -> explainTask(task, (Light) device);
            case Sensor -> explainTask(task, (Sensor) device);
            case Access -> explainTask(task, (Access) device);
            case CircuitBreak -> explainTask(task, (CircuitBreak) device);
            default -> throw new IllegalArgumentException("不支持的设备类型");
        };
    }

    public abstract T explainTask(Task task,AirCondition airCondition);
    public abstract T explainTask(Task task,Light light);
    public abstract T explainTask(Task task,Sensor sensor);
    public abstract T explainTask(Task task,Access access);
    public abstract T explainTask(Task task,CircuitBreak circuitBreak);

    protected final byte[] generatePayload(Task task,Device device){
        CommandLine commandLine = task.getCommandLine();
        Command command = commandLine.getCommand();
        Integer[] args = task.getArgs();
        Object[] toHexString = Arrays.stream(args).map(arg->{
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
            case SUM_SG: return SumChecker.generatePayload(orginalPayload);
            case CRC16: return CrcChecker.generatePayload(orginalPayload);
            case SUM_UNSG: return SumChecker.generateUnsignedBytePayload(orginalPayload);
            default:
                throw new IllegalArgumentException("不支持的校验类型");
        }
    }
    
}

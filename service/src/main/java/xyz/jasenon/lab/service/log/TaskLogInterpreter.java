package xyz.jasenon.lab.service.log;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.dto.task.TaskPriority;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.device.Device;
import xyz.jasenon.lab.service.aspect.LogInterpreter;
import xyz.jasenon.lab.service.dto.log.OperationLogParts;
import xyz.jasenon.lab.service.service.ILaboratoryService;
import xyz.jasenon.lab.service.strategy.device.DeviceFactory;

/**
 * 设备控制操作日志解释器：从 Task 解析教室、设备、操作内容，区分手动/自动。
 */
@Component
@RequiredArgsConstructor
public class TaskLogInterpreter implements LogInterpreter<Task> {

    private final ILaboratoryService laboratoryService;

    @Override
    public Object render(Task payload) {
        String operateWay = payload.getPriority() == TaskPriority.AUTOMATIC ? "自动" : "手动";
        String opContent = payload.getCommandLine() != null ? payload.getCommandLine().getDescription() : "控制设备";

        String room = null;
        String deviceName = null;
        try {
            Device device = DeviceFactory.getDeviceQMethod(payload.getDeviceType()).getDeviceById(payload.getDeviceId());
            if (device != null) {
                deviceName = device.getDeviceName();
                Long labId = device.getBelongToLaboratoryId();
                if (labId != null) {
                    Laboratory lab = laboratoryService.getById(labId);
                    room = lab != null ? (lab.getLaboratoryName() != null ? lab.getLaboratoryName() : lab.getLaboratoryId()) : String.valueOf(labId);
                }
            }
        } catch (Exception ignored) {
            // 解析失败时仅保留操作内容
        }
        if (deviceName == null) {
            deviceName = payload.getDeviceType() + "-" + payload.getDeviceId();
        }
        if (room == null) {
            room = "";
        }
        String paramDetail = null;
        try {
            if (payload.getCommandLine() != null && payload.getArgs() != null) {
                paramDetail = payload.getCommandLine().paramDetail(payload.getArgs());
            }
        } catch (Exception ignored) {
            // 参数解析异常时不追加参数描述，保持原样
        }
        String content = opContent
                + (paramDetail != null && !paramDetail.isEmpty() ? paramDetail : "")
                + (deviceName != null ? " [" + deviceName + "]" : "");
        return new OperationLogParts(room, deviceName, operateWay, content);
    }
}

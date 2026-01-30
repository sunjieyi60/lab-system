package xyz.jasenon.lab.service.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.device.gateway.RS485Gateway;
import xyz.jasenon.lab.common.entity.log.AlarmLog;
import xyz.jasenon.lab.common.entity.log.OperationLog;
import xyz.jasenon.lab.service.service.IAlarmLogService;
import xyz.jasenon.lab.service.service.IOperationLogService;
import xyz.jasenon.lab.service.service.ILaboratoryService;
import xyz.jasenon.lab.service.service.IRS485GatewayService;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

/**
 * 日志异步任务管理器：接收日志实体，封装为任务并交给线程池执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogTaskManager {

    private final ExecutorService logExecutor;
    private final IOperationLogService operationLogService;
    private final IAlarmLogService alarmLogService;
    private final IRS485GatewayService rs485GatewayService;
    private final ILaboratoryService laboratoryService;

    /**
     * 提交操作日志异步落库任务。
     */
    public void submitOperationLog(OperationLog logEntity) {
        if (logEntity == null) {
            return;
        }
        logExecutor.submit(() -> {
            try {
                operationLogService.save(logEntity);
            } catch (Exception e) {
                log.warn("保存操作日志失败: {}", logEntity, e);
            }
        });
    }

    /**
     * 提交报警日志异步落库任务。
     * 若 alarmTime 为空则填当前时间；若 room 为空且 gatewayId 不为空则按网关补全教室。
     */
    public void submitAlarmLog(AlarmLog logEntity) {
        if (logEntity == null) {
            return;
        }
        if (logEntity.getAlarmTime() == null) {
            logEntity.setAlarmTime(LocalDateTime.now());
        }
        if (logEntity.getRoom() == null && logEntity.getGatewayId() != null) {
            try {
                RS485Gateway gateway = rs485GatewayService.getById(logEntity.getGatewayId());
                if (gateway != null && gateway.getBelongToLaboratoryId() != null) {
                    Laboratory lab = laboratoryService.getById(gateway.getBelongToLaboratoryId());
                    String room = lab != null
                            ? (lab.getLaboratoryName() != null ? lab.getLaboratoryName() : lab.getLaboratoryId())
                            : String.valueOf(gateway.getBelongToLaboratoryId());
                    logEntity.setRoom(room);
                }
            } catch (Exception ignored) {
            }
        }
        logExecutor.submit(() -> {
            try {
                alarmLogService.save(logEntity);
            } catch (Exception e) {
                log.warn("保存报警日志失败: {}", logEntity, e);
            }
        });
    }
}


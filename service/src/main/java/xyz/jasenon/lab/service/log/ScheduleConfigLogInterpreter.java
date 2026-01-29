package xyz.jasenon.lab.service.log;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.service.aspect.LogInterpreter;
import xyz.jasenon.lab.service.quartz.model.*;
import xyz.jasenon.lab.service.service.ILaboratoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 报警联动设置操作日志解释器：记录教室、设备、报警条件、联动动作等。
 */
@Component
@RequiredArgsConstructor
public class ScheduleConfigLogInterpreter implements LogInterpreter<ScheduleConfigRoot> {

    private final ILaboratoryService laboratoryService;

    @Override
    public Object render(ScheduleConfigRoot payload) {
        ScheduleTask task = payload.getTask();
        if (task == null) {
            return "创建报警联动配置";
        }
        String taskName = task.getTaskName() != null ? task.getTaskName() : "未命名";
        String room = "";
        if (task.getLaboratoryId() != null) {
            Laboratory lab = laboratoryService.getById(task.getLaboratoryId());
            room = lab != null ? (lab.getLaboratoryName() != null ? lab.getLaboratoryName() : lab.getLaboratoryId()) : String.valueOf(task.getLaboratoryId());
        }
        List<String> conditionDesc = new ArrayList<>();
        if (payload.getConditionGroups() != null) {
            for (ConditionGroup cg : payload.getConditionGroups()) {
                if (cg.getConditions() != null) {
                    for (Condition c : cg.getConditions()) {
                        conditionDesc.add(c.getDesc() != null ? c.getDesc() : c.getExpr());
                    }
                }
            }
        }
        List<String> actionDesc = new ArrayList<>();
        if (payload.getActionGroups() != null) {
            for (ActionGroup ag : payload.getActionGroups()) {
                if (ag.getActions() != null) {
                    for (Action a : ag.getActions()) {
                        String op = a.getCommandLine() != null ? a.getCommandLine().getDescription() : "控制";
                        actionDesc.add(op + " 设备" + a.getDeviceType() + "-" + a.getDeviceId());
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("任务名: ").append(taskName);
        if (!room.isEmpty()) {
            sb.append(", 教室: ").append(room);
        }
        if (!conditionDesc.isEmpty()) {
            sb.append(", 条件: ").append(String.join("; ", conditionDesc));
        }
        if (!actionDesc.isEmpty()) {
            sb.append(", 联动动作: ").append(String.join("; ", actionDesc));
        }
        if (payload.getAlarmGroup() != null && !payload.getAlarmGroup().isEmpty()) {
            sb.append(", 报警方式: ").append(payload.getAlarmGroup().stream()
                    .map(a -> a.getType() != null ? a.getType().name() : "")
                    .collect(Collectors.joining(", ")));
        }
        return sb.toString();
    }
}

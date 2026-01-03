package xyz.jasenon.lab.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.schedule.model.ConfigRoot;
import xyz.jasenon.lab.schedule.model.RunResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskRuntimeService {

    private final ConfigLoader configLoader;
    private final TimeCheckService timeCheckService;
    private final ConditionService conditionService;

    public RunResult runOnce(Long groupId) {
        ConfigRoot cfg = configLoader.loadByGroupId(groupId);
        RunResult result = new RunResult();
        // 时间规则
        if (!timeCheckService.pass(cfg.getTaskGroup().getTimeRule())) {
            result.setPassed(false);
            result.setActionLogs(List.of("时间条件未通过，跳过执行"));
            return result;
        }
        // 条件评估
        var cond = conditionService.evaluate(cfg.getConditionGroup(), cfg.getDataGroups());
        if (!cond.isPassed()) {
            List<String> logs = new ArrayList<>();
            cond.getDetails().forEach(d -> logs.add("条件#" + d.getId() + " = " + d.getResult()));
            logs.add("条件未通过，跳过执行");
            result.setPassed(false);
            result.setActionLogs(logs);
            return result;
        }
        // 模拟执行动作
        List<String> logs = new ArrayList<>();
        cfg.getActions().stream()
                .sorted(Comparator.comparing(a -> a.getOrder() == null ? 0 : a.getOrder()))
                .forEach(a -> logs.add("执行动作: " + a.getName() + " [" + a.getType() + "] payload=" + a.getPayload()));
        result.setPassed(true);
        result.setActionLogs(logs);
        return result;
    }
}


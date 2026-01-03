package xyz.jasenon.lab.schedule.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jasenon.lab.schedule.model.ConfigRoot;
import xyz.jasenon.lab.schedule.model.RunResult;
import xyz.jasenon.lab.schedule.service.ConditionService;
import xyz.jasenon.lab.schedule.service.ConfigLoader;
import xyz.jasenon.lab.schedule.service.TaskRuntimeService;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final ConfigLoader configLoader;
    private final ConditionService conditionService;
    private final TaskRuntimeService taskRuntimeService;

    @GetMapping("/config")
    public ConfigRoot config() {
        return configLoader.current();
    }

    @PostMapping("/evaluate")
    public ConditionService.ConditionSummary evaluate(Long groupId) {
        ConfigRoot cfg = configLoader.loadByGroupId(groupId == null ? 1L : groupId);
        return conditionService.evaluate(cfg.getConditionGroup(), cfg.getDataGroups());
    }

    @PostMapping("/run")
    public RunResult run(Long groupId) {
        return taskRuntimeService.runOnce(groupId == null ? 1L : groupId);
    }
}


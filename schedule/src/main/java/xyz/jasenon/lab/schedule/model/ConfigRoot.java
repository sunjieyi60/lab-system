package xyz.jasenon.lab.schedule.model;

import lombok.Data;
import java.util.List;

@Data
public class ConfigRoot {
    private TaskGroupConfig taskGroup;
    private List<DataGroup> dataGroups;
    private ConditionGroup conditionGroup;
    private List<Action> actions;
    private AlertConfig alerts;
}



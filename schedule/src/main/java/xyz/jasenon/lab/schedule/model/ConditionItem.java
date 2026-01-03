package xyz.jasenon.lab.schedule.model;

import lombok.Data;

@Data
public class ConditionItem {
    private Long id;
    private String expr;
    private String desc;
    private Long dataGroupId;
}



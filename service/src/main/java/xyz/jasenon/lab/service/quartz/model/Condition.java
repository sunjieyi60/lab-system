package xyz.jasenon.lab.service.quartz.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
public class Condition {
    /**
     * 条件ID
     */
    private String id;

    /**
     * 条件表达式
     */
    private String expr;

    /**
     * 条件描述
     */
    private String desc;

    /**
     * 条件组ID
     */
    private String conditionGroupId;
}

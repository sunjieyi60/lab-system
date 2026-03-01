package xyz.jasenon.lab.service.quartz.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
@Getter
@Setter
public class Alarm {

    /**
     * 报警ID
     */
    private String id;

    /**
     * 任务组id
     */
    private String scheduleTaskId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 报警类型
     */
    private AlarmType type;

    public enum AlarmType {
        SMS,
        SMTP;
    }

}

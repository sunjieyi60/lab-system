package xyz.jasenon.lab.service.quartz.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
     * 用户ID
     */
    private Long userId;

    /**
     * 报警类型
     */
    private AlarmType type;

    /**
     * 报警分组ID
     */
    private String alarmGroupId;

    enum AlarmType {
        SMS,
        SMTP;
    }

}

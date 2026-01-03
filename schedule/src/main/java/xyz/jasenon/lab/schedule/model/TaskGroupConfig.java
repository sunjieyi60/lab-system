package xyz.jasenon.lab.schedule.model;

import lombok.Data;

@Data
public class TaskGroupConfig {
    private Long id;
    private String name;
    private String cron;
    private boolean enable;
    private String misfirePolicy;
    private String descText;
    private TimeRule timeRule;
}



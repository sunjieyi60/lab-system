package xyz.jasenon.lab.schedule.model;

import lombok.Data;
import java.util.Map;

@Data
public class Action {
    private Long id;
    private Long groupId;
    private String name;
    private String type; // SMS / SMTP / HTTP ...
    private Map<String, Object> payload;
    private Integer order;
    private Integer retryTimes;
    private Integer retryBackoffMs;
    private Integer timeoutMs;
    private String parallelTag;
    private Boolean enable;
}



package xyz.jasenon.lab.schedule.model;

import lombok.Data;
import java.util.Map;

@Data
public class AlertChannel {
    private Long id;
    private String type; // SMS / SMTP
    private Map<String, Object> config;
}



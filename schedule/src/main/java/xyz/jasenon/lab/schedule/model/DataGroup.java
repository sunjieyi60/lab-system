package xyz.jasenon.lab.schedule.model;

import lombok.Data;
import java.util.Map;

@Data
public class DataGroup {
    private Long id;
    private String name;
    private String type; // TEMPERATURE / LIGHT / ...
    private Map<String, Object> fetchConfig;
    private String agg;
    private Double mockValue; // demo用
    private Boolean enable;
}



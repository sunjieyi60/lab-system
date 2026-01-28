package xyz.jasenon.lab.schedule.model;

import lombok.Data;
import java.util.List;

@Data
public class AlertStrategy {
    private boolean enable;
    private Long throttleMs;
    private Integer continuousFailThreshold;
    private List<Long> bindChannels;
}




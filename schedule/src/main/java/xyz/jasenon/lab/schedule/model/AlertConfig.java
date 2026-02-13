package xyz.jasenon.lab.schedule.model;

import lombok.Data;
import java.util.List;

@Data
public class AlertConfig {
    private List<AlertChannel> channels;
    private AlertStrategy strategy;
}








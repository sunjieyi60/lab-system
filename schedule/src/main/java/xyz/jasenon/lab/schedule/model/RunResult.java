package xyz.jasenon.lab.schedule.model;

import lombok.Data;
import java.util.List;

@Data
public class RunResult {
    private boolean passed;
    private List<String> actionLogs;
}








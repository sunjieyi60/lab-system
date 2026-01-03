package xyz.jasenon.lab.schedule.model;

import lombok.Data;
import java.util.List;

@Data
public class TimeRule {
    private Long id;
    private Long groupId;
    private List<String> businessTime;
    private List<Integer> weekdays;
    private String weekParity; // ALL/ODD/EVEN
    private List<String> dateRange; // [start, end]
    private String timezone;
}



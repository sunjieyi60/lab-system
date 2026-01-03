package xyz.jasenon.lab.schedule.model;

import lombok.Data;
import java.util.List;

@Data
public class ConditionGroup {
    private Long id;
    private Long groupId;
    private Boolean enable;
    private String logic; // ALL / ANY
    private List<ConditionItem> conditions;
    private TimeRule timeRule; // 可选扩展
}



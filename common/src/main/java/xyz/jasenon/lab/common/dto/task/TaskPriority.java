package xyz.jasenon.lab.common.dto.task;

import lombok.Getter;

@Getter
public enum TaskPriority {
    NORMAL(0),
    AUTOMATIC(1),
    POLLING(1);

    private final int priority;
    TaskPriority(int priority) {
        this.priority = priority;
    }
}

package xyz.jasenon.lab.service.dto.course;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
public class DeleteCourseSchedule {

    /**
     * 课程表ID
     */
    @NotNull(message = "课程表ID不能为空")
    private Long courseScheduleId;

}

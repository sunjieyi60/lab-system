package xyz.jasenon.lab.service.dto.course;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 编辑课程表（仅修改 weekdays）。
 *
 * @author Jasenon_ce
 * @date 2026/03/20
 */
@Getter
@Setter
@Accessors(chain = true)
public class EditCourseSchedule {

    /**
     * 课程表ID
     */
    @NotNull(message = "课程表ID不能为空")
    private Long courseScheduleId;

    /**
     * 上课星期 1..7，周一到周日
     */
    @NotEmpty(message = "weekdays不能为空")
    private List<Integer> weekdays;
}


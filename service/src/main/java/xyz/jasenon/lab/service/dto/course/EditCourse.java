package xyz.jasenon.lab.service.dto.course;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
public class EditCourse {

    /**
     * 课程ID
     */
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程容量
     */
    private Integer volume;

    @Pattern(regexp = "^[0-9]{4}级", message = "年级格式不正确")
    private String grade;

}

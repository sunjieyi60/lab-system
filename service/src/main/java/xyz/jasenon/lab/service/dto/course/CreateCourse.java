package xyz.jasenon.lab.service.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
@Accessors(chain = true)
public class CreateCourse {

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private Integer volume;

    @Pattern(regexp = "^[0-9]{4}级", message = "年级格式不正确")
    private String grade;

}

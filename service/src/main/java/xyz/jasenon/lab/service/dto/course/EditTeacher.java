package xyz.jasenon.lab.service.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;

@Data
public class EditTeacher {

    @NotNull(message = "教师id不能为空")
    private Long teacherId;

    @NotBlank(message = "教师名称不能为空")
    private String teacherName;

}

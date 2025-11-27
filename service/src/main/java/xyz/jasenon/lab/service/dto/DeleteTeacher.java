package xyz.jasenon.lab.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
public class DeleteTeacher {

    /**
     * 教师ID
     */
    @NotNull
    private Long teacherId;

}

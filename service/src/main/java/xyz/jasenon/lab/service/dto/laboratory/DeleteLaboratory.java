package xyz.jasenon.lab.service.dto.laboratory;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
public class DeleteLaboratory {

    /**
     * 实验室id
     */
    @NotNull(message = "实验室ID不能为空")
    private Long id;

}

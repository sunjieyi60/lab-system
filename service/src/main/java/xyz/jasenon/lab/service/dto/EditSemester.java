package xyz.jasenon.lab.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.service.time_order.TimeOrder;

import java.time.LocalDate;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
@Accessors(chain = true)
public class EditSemester {

    /**
     * 学期id
     */
    @NotNull
    private Long semesterId;

    /**
     * 学期名称
     */
    @Pattern(regexp = "(\\d{4})-(\\d{4}) 第(\\d+)学年", message = "学期名称  存在正则(\\d{4})-(\\d{4}) 第(\\d+)学年")
    private String name;

    /**
     * 开始时间
     */
    @TimeOrder(order = 0)
    @NotNull(message = "开始时间不能为空")
    private LocalDate startDate;

    /**
     * 结束时间
     */
    @TimeOrder(order = 1)
    @NotNull(message = "结束时间不能为空")
    private LocalDate endDate;

}

package xyz.jasenon.lab.service.dto.course;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.class_time_table.WeekType;
import xyz.jasenon.lab.service.time_order.TimeOrder;

import java.time.LocalTime;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
@Accessors(chain = true)
public class CreateSchedule {

    /**
     * 学期ID
     */
    @NotNull
    private Long semesterId;

    /**
     * 实验室ID
     */
    @NotNull
    private Long laboratoryId;

    /**
     * 上课周类型
     */
    @NotNull
    private WeekType weekType;

    /**
     * 开始周
     */
    @NotNull
    private Integer startWeek;

    /**
     * 结束周
     */
    @NotNull
    private Integer endWeek;

    /**
     * 开始时间
     */
    @TimeOrder(order = 0)
    @NotNull
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @TimeOrder(order = 1)
    @NotNull
    private LocalTime endTime;

    /**
     * 上课周几
     */
    @NotEmpty
    private List<Integer> weekdays;

}

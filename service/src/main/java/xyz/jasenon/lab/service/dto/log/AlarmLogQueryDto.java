package xyz.jasenon.lab.service.dto.log;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 报警日志分页查询条件（不含分页参数，pageNum/pageSize 由 @RequestParam 单独传）。
 */
@Getter
@Setter
public class AlarmLogQueryDto {

    /** 时间起，必填。格式：yyyy-M-d-HH:mm */
    @NotNull(message = "时间起不能为空")
    @DateTimeFormat(pattern = "yyyy-M-d-HH:mm")
    private LocalDateTime startTime;

    /** 时间止，必填。格式：yyyy-M-d-HH:mm */
    @NotNull(message = "时间止不能为空")
    @DateTimeFormat(pattern = "yyyy-M-d-HH:mm")
    private LocalDateTime endTime;

    /** 报警大类（多选，IN 查询）；不传或空则不按大类过滤 */
    private List<String> categories;

    /** 报警类型（多选，IN 查询）：门禁报警 / 电气报警 / 空调故障 / 环境报警 等；不传或空则不按类型过滤 */
    private List<String> alarmTypes;

    /** 教室 */
    private String room;

    @AssertTrue(message = "时间止必须不早于时间起")
    public boolean isEndTimeNotBeforeStartTime() {
        if (startTime == null || endTime == null) return true;
        return !endTime.isBefore(startTime);
    }
}

package xyz.jasenon.lab.service.dto.log;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 操作日志分页查询条件（不含分页参数，pageNum/pageSize 由 @RequestParam 单独传）。
 */
@Getter
@Setter
public class OperationLogQueryDto {

    /** 时间起，必填。格式：yyyy-M-d-HH:mm */
    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(pattern = "yyyy-M-d-HH:mm")
    private LocalDateTime startTime;

    /** 时间止，必填。格式：yyyy-M-d-HH:mm */
    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(pattern = "yyyy-M-d-HH:mm")
    private LocalDateTime endTime;

    /** 操作类型（多选，IN 查询）；不传或空则不按操作类型过滤 */
    private List<String> logTypes;

    /** 账号 */
    private String account;

    /** 姓名 */
    private String name;

    /** 教室 */
    private String room;

    /** 设备 */
    private String device;

    @AssertTrue(message = "时间止必须不早于时间起")
    public boolean isEndTimeNotBeforeStartTime() {
        if (startTime == null || endTime == null) return true;
        return !endTime.isBefore(startTime);
    }
}

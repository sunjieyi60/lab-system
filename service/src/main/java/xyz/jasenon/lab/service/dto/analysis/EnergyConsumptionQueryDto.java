package xyz.jasenon.lab.service.dto.analysis;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 能耗统计查询：时间范围 + 楼栋/单位/实验室/智能空开筛选。
 */
@Getter
@Setter
@Accessors(chain = true)
public class EnergyConsumptionQueryDto {

    /**
     * 时间起始
     */
    @NotNull(message = "统计开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 时间结束
     */
    @NotNull(message = "统计结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 楼栋
     */
    private Long buildingId;

    /**
     * 部门筛选
     */
    private Long deptId;

    /**
     * 实验室筛选
     */
    private List<Long> laboratoryIds;

    /**
     * 设备筛选
     */
    private List<Long> deviceIds;
}

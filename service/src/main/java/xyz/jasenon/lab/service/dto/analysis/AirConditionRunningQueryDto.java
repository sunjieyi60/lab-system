package xyz.jasenon.lab.service.dto.analysis;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空调运行时长统计查询：时间范围 + 楼栋/单位/实验室/设备筛选。
 */
@Getter
@Setter
@Accessors(chain = true)
public class AirConditionRunningQueryDto {

    /**
     * 统计开始时间（必填，适配格式：yyyy-MM-dd HH:mm）
     */
    @NotNull(message = "统计开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 统计结束时间（必填，适配格式：yyyy-MM-dd HH:mm）
     */
    @NotNull(message = "统计结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 所在楼栋 id，空=全部
     */
    private Long buildingId;

    /**
     * 所属单位/部门 id，空=全部
     */
    private Long deptId;

    /**
     * 实验室 id 列表，空或空列表=全部
     */
    private List<Long> laboratoryIds;

    /**
     * 空调设备 id 列表，空或空列表=不按设备过滤
     */
    private List<Long> deviceIds;
}

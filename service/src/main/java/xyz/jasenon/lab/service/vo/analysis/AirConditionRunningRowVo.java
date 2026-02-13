package xyz.jasenon.lab.service.vo.analysis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 空调运行时长统计：单行（列表显示用）。
 */
@Getter
@Setter
@Accessors(chain = true)
public class AirConditionRunningRowVo {

    /**
     * 时间范围文案，如 "2025-09-01 8:00 至 2025-09-01 21:00"
     */
    private String timeRange;

    /**
     * 实验室编号
     */
    private String laboratoryNo;

    /**
     * 所属单位名称
     */
    private String deptName;

    /**
     * 空调内机名称/编号
     */
    private String acUnitName;

    /**
     * 运行时长（小时）
     */
    private BigDecimal durationHours;

    /**
     * 占比（百分比），如 "77.8"
     */
    private String proportion;

    /**
     * 数据来源：mysql / mysql_and_redis（部分时段由 Redis 补段）
     */
    private String dataSource;
}

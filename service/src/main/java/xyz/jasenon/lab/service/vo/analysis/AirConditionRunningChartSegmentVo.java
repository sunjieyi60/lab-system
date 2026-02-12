package xyz.jasenon.lab.service.vo.analysis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 空调运行时长饼图：单个扇区。
 */
@Getter
@Setter
@Accessors(chain = true)
public class AirConditionRunningChartSegmentVo {

    /**
     * 空调内机名称/编号
     */
    private String name;

    /**
     * 运行时长（小时）
     */
    private BigDecimal hours;

    /**
     * 占比（百分比数值）
     */
    private BigDecimal proportion;
}

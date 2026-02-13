package xyz.jasenon.lab.service.vo.analysis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 空调运行时长统计接口返回：列表 + 饼图数据。
 */
@Getter
@Setter
@Accessors(chain = true)
public class AirConditionRunningResultVo {

    /**
     * 运行总时长（小时）
     */
    private BigDecimal totalHours;

    /**
     * 列表行（列表显示）
     */
    private List<AirConditionRunningRowVo> list;

    /**
     * 饼图扇区（饼图显示）
     */
    private List<AirConditionRunningChartSegmentVo> chartSegments;

    /**
     * 汇总行：时间范围、实验室/单位/内机汇总、总时长、100%
     */
    private AirConditionRunningSummaryVo summaryRow;

    /**
     * 可选提示：如「部分设备该时段无历史开关记录，未计入统计」
     */
    private String warningMessage;
}

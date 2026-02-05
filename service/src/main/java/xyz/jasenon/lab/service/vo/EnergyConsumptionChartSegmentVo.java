package xyz.jasenon.lab.service.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 能耗饼图：单个扇区。
 */
@Getter
@Setter
@Accessors(chain = true)
public class EnergyConsumptionChartSegmentVo {

    /**
     * 智能空开名称
     */
    private String name;

    /**
     * 电能(Kwh)
     */
    private BigDecimal kwh;

    /**
     * 占比（百分比数值）
     */
    private BigDecimal proportion;
}

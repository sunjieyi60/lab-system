package xyz.jasenon.lab.service.service;

import xyz.jasenon.lab.service.dto.analysis.EnergyConsumptionQueryDto;
import xyz.jasenon.lab.service.vo.analysis.EnergyConsumptionResultVo;

/**
 * 能耗统计：按时间范围首尾读数相减得电能(Kwh)，返回列表+饼图+汇总行。
 */
public interface IEnergyConsumptionAnalysisService {

    /**
     * 按时间范围及筛选条件统计能耗（首尾 energy 相减），返回总能耗、明细列表、饼图扇区、汇总行。
     */
    EnergyConsumptionResultVo getEnergyConsumption(EnergyConsumptionQueryDto query);
}

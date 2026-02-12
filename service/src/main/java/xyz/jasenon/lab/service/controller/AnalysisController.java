package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.analysis.AirConditionRunningQueryDto;
import xyz.jasenon.lab.service.dto.analysis.AnalysisQueryDto;
import xyz.jasenon.lab.service.dto.analysis.EnergyConsumptionQueryDto;
import xyz.jasenon.lab.service.service.IEnergyConsumptionAnalysisService;
import xyz.jasenon.lab.service.service.IAirConditionRunningAnalysisService;
import xyz.jasenon.lab.service.service.IAnalysisService;
import xyz.jasenon.lab.service.vo.analysis.AirConditionRunningResultVo;
import xyz.jasenon.lab.service.vo.analysis.AnalysisChartVo;
import xyz.jasenon.lab.service.vo.analysis.EnergyConsumptionResultVo;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;

/**
 * 数据分析：课程数、学时数、人学时数及分布，供前端画图。
 */
@Api("数据分析")
@RestController
@RequestMapping("/analysis")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AnalysisController {

    @Autowired
    private IAnalysisService analysisService;
    @Autowired
    private IAirConditionRunningAnalysisService airConditionRunningAnalysisService;
    @Autowired
    private IEnergyConsumptionAnalysisService energyConsumptionAnalysisService;

    @RequestPermission(allowed = {Permissions.ACADEMIC_AFFAIRS_ANALYSIS})
    @PostMapping("/chart")
    @ApiOperation("获取教务数据")
    public R<AnalysisChartVo> getChartData(@RequestBody AnalysisQueryDto query) {
        return analysisService.getChartData(query != null ? query : new AnalysisQueryDto());
    }

    @RequestPermission(allowed = {Permissions.LABORATORY_CENTRAL_AIRCONDITION})
    @PostMapping("/air-condition/running")
    @ApiOperation("空调运行时长统计")
    public R<AirConditionRunningResultVo> getAirConditionRunningStats(@RequestBody(required = false) AirConditionRunningQueryDto query) {
        return airConditionRunningAnalysisService.getRunningStats(query != null ? query : new AirConditionRunningQueryDto());
    }

    @RequestPermission(allowed = {Permissions.LABORATORY_POWER_CONSUMPTION})
    @PostMapping("/energy-consumption")
    @ApiOperation("能耗统计")
    public R<EnergyConsumptionResultVo> getEnergyConsumption(@RequestBody(required = false) EnergyConsumptionQueryDto query) {
        return energyConsumptionAnalysisService.getEnergyConsumption(query != null ? query : new EnergyConsumptionQueryDto());
    }
}

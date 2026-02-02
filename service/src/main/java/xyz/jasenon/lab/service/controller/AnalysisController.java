package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.analysis.AnalysisQueryDto;
import xyz.jasenon.lab.service.service.IAnalysisService;
import xyz.jasenon.lab.service.vo.AnalysisChartVo;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;

/**
 * 数据分析：课程数、学时数、人学时数及分布，供前端画图。
 */
@Api("教务数据分析")
@RestController
@RequestMapping("/analysis")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AnalysisController {

    @Autowired
    private IAnalysisService analysisService;

    @RequestPermission(allowed = {Permissions.ACADEMIC_AFFAIRS_ANALYSIS})
    @PostMapping("/chart")
    @ApiOperation("获取图表数据")
    public R<AnalysisChartVo> getChartData(@RequestBody AnalysisQueryDto query) {
        return analysisService.getChartData(query != null ? query : new AnalysisQueryDto());
    }
}

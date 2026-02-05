package xyz.jasenon.lab.service.service;

import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.analysis.AnalysisQueryDto;
import xyz.jasenon.lab.service.vo.AnalysisChartVo;

/**
 * 数据分析：课程数、学时数、人学时数及按周次/星期/节次分布，供前端画图。
 */
public interface IAnalysisService {

    /**
     * 按筛选条件统计并返回图表数据。
     *
     * @param query 学年学期、楼栋、部门、实验室（均为可选）
     * @return 总数 + byWeek / byWeekday / bySection 分布
     */
    R<AnalysisChartVo> getChartData(AnalysisQueryDto query);

    /**
     * 使图表缓存失效。排课/课程表增删改后调用，保证下次图表请求从数据库重新聚合。
     */
    void invalidateChartCache();
}

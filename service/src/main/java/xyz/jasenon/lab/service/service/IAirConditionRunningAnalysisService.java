package xyz.jasenon.lab.service.service;

import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.analysis.AirConditionRunningQueryDto;
import xyz.jasenon.lab.service.vo.analysis.AirConditionRunningResultVo;

/**
 * 空调运行时长统计：遍历 Redis 在线状态 + DB 开启时间区间，切分后多线程汇总。
 */
public interface IAirConditionRunningAnalysisService {

    /**
     * 按时间范围及筛选条件统计运行时长，返回列表 + 饼图数据。
     * 数据来源：Redis 当前状态 + DB 历史记录合并为开启区间，按查询区间切分后线程安全汇总。
     *
     * @param query 时间范围、楼栋、单位、实验室、设备（均可选）
     * @return 总时长、列表行、饼图扇区
     */
    R<AirConditionRunningResultVo> getRunningStats(AirConditionRunningQueryDto query);
}

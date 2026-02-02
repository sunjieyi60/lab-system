package xyz.jasenon.lab.service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import xyz.jasenon.lab.service.vo.DimensionPointVo;

import java.util.List;

/**
 * 数据分析聚合：按周次、星期、节次在 DB 内 GROUP BY，只返回汇总行，避免全量拉取。
 */
@Mapper
public interface AnalysisMapper {

    /**
     * 按 start_week 聚合：label=周次，courseCount/sectionCount/studentSectionCount
     */
    List<DimensionPointVo> aggByWeek(
            @Param("semesterId") Long semesterId,
            @Param("deptId") Long deptId,
            @Param("labIds") List<Long> labIds
    );

    /**
     * 按 weekdays 展开后按星期聚合：label=1..7（周一至周日）
     */
    List<DimensionPointVo> aggByWeekday(
            @Param("semesterId") Long semesterId,
            @Param("deptId") Long deptId,
            @Param("labIds") List<Long> labIds
    );

    /**
     * 按节次槽位聚合：label=0..5（对应 1-2、3-4…11-12 节）
     */
    List<DimensionPointVo> aggBySectionSlot(
            @Param("semesterId") Long semesterId,
            @Param("deptId") Long deptId,
            @Param("labIds") List<Long> labIds
    );
}

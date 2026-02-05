package xyz.jasenon.lab.service.dto.analysis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 数据分析查询条件：学年学期、所在楼栋、所属单位、实验室（可多选）。
 * 均为可选，不传或空表示“全部”。
 */
@Getter
@Setter
@Accessors(chain = true)
public class AnalysisQueryDto {

    /**
     * 学年学期 id，空=全部
     */
    private Long semesterId;

    /**
     * 所在楼栋 id，空=全部
     */
    private Long buildingId;

    /**
     * 所属单位/部门 id，空=全部
     */
    private Long deptId;

    /**
     * 实验室 id 列表（laboratory.id），空或空列表=全部
     */
    private List<Long> laboratoryIds;
}

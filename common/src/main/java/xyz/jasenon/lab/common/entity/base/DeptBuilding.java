package xyz.jasenon.lab.common.entity.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Getter
@Setter
@Accessors(fluent = true)
public class DeptBuilding extends BaseEntity {

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 楼栋id
     */
    private Long buildingId;

}

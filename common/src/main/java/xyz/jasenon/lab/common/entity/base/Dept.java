package xyz.jasenon.lab.common.entity.base;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.BaseEntity;

@Getter
@Setter
public class Dept extends BaseEntity {

    /**
     * 部门名称
     */
    private String deptName;

}

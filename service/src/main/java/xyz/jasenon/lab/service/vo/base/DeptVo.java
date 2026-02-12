package xyz.jasenon.lab.service.vo.base;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.base.Building;
import xyz.jasenon.lab.common.entity.base.Dept;

import java.util.List;

@Getter
@Setter
public class DeptVo {

    /**
     * 部门信息
     */
    private Dept dept;

    /**
     * 部门下的所有楼栋信息
     */
    private List<Building> buildings;

}

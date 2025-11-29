package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.Building;
import xyz.jasenon.lab.common.entity.base.DeptBuilding;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.DeleteBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;
import xyz.jasenon.lab.service.mapper.BuildingMapper;
import xyz.jasenon.lab.service.mapper.DeptBuildingMapper;
import xyz.jasenon.lab.service.service.IBuildingService;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements IBuildingService {

    @Autowired
    private DeptBuildingMapper deptBuildingMapper;

    @Override
    public R createBuilding(CreateBuilding createBuilding) {

        Building building = new Building();
        building.setBuildingName(createBuilding.getBuildingName());
        this.save(building);

        for (Long deptId : createBuilding.getDeptIds()){
            DeptBuilding deptBuilding = new DeptBuilding();
            deptBuilding.setDeptId(deptId);
            deptBuilding.setBuildingId(building.getId());
            deptBuildingMapper.insert(deptBuilding);
        }
        return R.success("楼栋创建成功");
    }

    @Override
    public R editBuilding(EditBuilding editBuilding) {
        // TODO 应当限制用户只能修改自己所属部门下的楼栋  交给后人实现
        Building building = this.getById(editBuilding.getBuildingId());
        if (building == null) {
            return R.fail("楼栋不存在");
        }
        building.setBuildingName(editBuilding.getBuildingName());
        this.updateById(building);
        return R.success("楼栋修改成功");
    }

    @Override
    public R deleteBuilding(DeleteBuilding deleteBuilding) {
        // TODO 应当限制用户只能删除自己所属部门下的楼栋  交给后人实现
        this.removeById(deleteBuilding.getBuildingId());
        return R.success();
    }
}

package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.Building;
import xyz.jasenon.lab.common.entity.base.DeptBuilding;
import xyz.jasenon.lab.common.entity.base.DeptUser;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.DeleteBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;
import xyz.jasenon.lab.service.mapper.BuildingMapper;
import xyz.jasenon.lab.service.mapper.DeptBuildingMapper;
import xyz.jasenon.lab.service.mapper.DeptUserMapper;
import xyz.jasenon.lab.service.mapper.LaboratoryMapper;
import xyz.jasenon.lab.service.service.IBuildingService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements IBuildingService {

    @Autowired
    private DeptBuildingMapper deptBuildingMapper;
    @Autowired
    private DeptUserMapper deptUserMapper;
    @Autowired
    private LaboratoryMapper laboratoryMapper;

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
        if (!isBuildingInOperatorDepts(editBuilding.getBuildingId())) {
            return R.fail("部门越权");
        }
        building.setBuildingName(editBuilding.getBuildingName());
        this.updateById(building);
        return R.success("楼栋修改成功");
    }

    @Override
    public R deleteBuilding(DeleteBuilding deleteBuilding) {
        // TODO 应当限制用户只能删除自己所属部门下的楼栋  交给后人实现
        if (!this.lambdaQuery().eq(Building::getId, deleteBuilding.getBuildingId()).exists()) {
            return R.fail("楼栋不存在");
        }
        if (!isBuildingInOperatorDepts(deleteBuilding.getBuildingId())) {
            return R.fail("部门越权");
        }
        Long buildingId = deleteBuilding.getBuildingId();
        laboratoryMapper.delete(new LambdaQueryWrapper<Laboratory>().eq(Laboratory::getBelongToBuilding, buildingId));
        deptBuildingMapper.delete(new LambdaQueryWrapper<DeptBuilding>().eq(DeptBuilding::getBuildingId, buildingId));
        this.removeById(buildingId);
        return R.success();
    }

    /**
     * 判断楼栋是否属于当前操作人所属部门下的楼栋（仅允许操作自己所属部门的楼栋）
     */
    private boolean isBuildingInOperatorDepts(Long buildingId) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        List<Long> operatorDeptIds = deptUserMapper.selectList(
                new LambdaQueryWrapper<DeptUser>().eq(DeptUser::getUserId, operatorId)
        ).stream().map(DeptUser::getDeptId).toList();
        if (operatorDeptIds.isEmpty()) {
            return false;
        }
        Set<Long> allowedBuildingIds = deptBuildingMapper.selectList(
                new LambdaQueryWrapper<DeptBuilding>().in(DeptBuilding::getDeptId, operatorDeptIds)
        ).stream().map(DeptBuilding::getBuildingId).collect(Collectors.toSet());
        return allowedBuildingIds.contains(buildingId);
    }
}

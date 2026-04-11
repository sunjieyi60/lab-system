package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.DiyResponseEntity;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.annotation.log.LogPoint;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.DeleteBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;
import xyz.jasenon.lab.service.service.IBuildingService;
import xyz.jasenon.lab.common.entity.base.Building;

import java.util.List;


/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Api("楼栋")
@RestController
@RequestMapping("/building")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class BuildingController {

    @Autowired
    private IBuildingService buildingService;

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PostMapping("/create")
    @ApiOperation("创建楼栋")
    @LogPoint(title = "'楼栋管理'", sqEl = "#createBuilding", clazz = CreateBuilding.class)
    public DiyResponseEntity<R<Building>> create(@Validated @RequestBody CreateBuilding createBuilding) {
        return DiyResponseEntity.of(R.success(buildingService.createBuilding(createBuilding)));
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/edit")
    @ApiOperation("编辑楼栋")
    @LogPoint(title = "'楼栋管理'", sqEl = "#editBuilding", clazz = EditBuilding.class)
    public DiyResponseEntity<R<Building>> edit(@Validated @RequestBody EditBuilding editBuilding) {
        return DiyResponseEntity.of(R.success(buildingService.editBuilding(editBuilding)));
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @DeleteMapping("/delete")
    @ApiOperation("删除楼栋")
    @LogPoint(title = "'楼栋管理'", sqEl = "#deleteBuilding", clazz = DeleteBuilding.class)
    public DiyResponseEntity<R<Void>> delete(@Validated @RequestBody DeleteBuilding deleteBuilding) {
        buildingService.deleteBuilding(deleteBuilding);
        return DiyResponseEntity.of(R.success());
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD, Permissions.BASE_VIEW})
    @GetMapping("/list")
    @ApiOperation("获取楼栋列表")
    public DiyResponseEntity<R<List<Building>>> listBuilding() {
        return DiyResponseEntity.of(R.success(buildingService.listBuilding()));
    }

}

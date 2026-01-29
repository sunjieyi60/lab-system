package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.annotation.log.LogPoint;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;
import xyz.jasenon.lab.service.service.IBuildingService;


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
    @LogPoint(title = "楼栋管理", sqEl = "#createBuilding", clazz = CreateBuilding.class)
    public R create(@Validated @RequestBody CreateBuilding createBuilding) {
        return buildingService.createBuilding(createBuilding);
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/edit")
    @ApiOperation("编辑楼栋")
    @LogPoint(title = "楼栋管理", sqEl = "#editBuilding", clazz = EditBuilding.class)
    public R edit(@Validated @RequestBody EditBuilding editBuilding) {
        return buildingService.editBuilding(editBuilding);
    }

}

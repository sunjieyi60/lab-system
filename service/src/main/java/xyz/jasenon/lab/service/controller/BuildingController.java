package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;
import xyz.jasenon.lab.service.service.IBuildingService;


/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@RestController
@RequestMapping("/building")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class BuildingController {

    @Autowired
    private IBuildingService buildingService;

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PostMapping("/create")
    public R create(@RequestBody CreateBuilding createBuilding) {
        return buildingService.createBuilding(createBuilding);
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/edit")
    public R edit(@RequestBody EditBuilding editBuilding) {
        return buildingService.editBuilding(editBuilding);
    }

}

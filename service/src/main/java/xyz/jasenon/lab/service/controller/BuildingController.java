package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.CreateBuilding;
import xyz.jasenon.lab.service.dto.EditBuilding;
import xyz.jasenon.lab.service.service.IBuildingService;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@RestController
@RequestMapping("/building")
@CrossOrigin("*")
public class BuildingController {

    @Autowired
    private IBuildingService buildingService;

    @PostMapping("/create")
    public R create(@RequestBody CreateBuilding createBuilding) {
        return buildingService.createBuilding(createBuilding);
    }

    @PutMapping("/edit")
    public R edit(@RequestBody EditBuilding editBuilding) {
        return buildingService.editBuilding(editBuilding);
    }

}

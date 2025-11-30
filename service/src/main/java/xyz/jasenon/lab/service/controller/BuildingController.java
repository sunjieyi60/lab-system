package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;
import xyz.jasenon.lab.service.service.IBuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

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

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PostMapping("/create")
    @Operation(summary = "创建楼栋", requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
            examples = @ExampleObject(name = "CreateBuilding", value = "{\n  \"buildingName\": \"A栋\",\n  \"deptIds\": [301,302]\n}"))))
    public R create(@RequestBody CreateBuilding createBuilding) {
        return buildingService.createBuilding(createBuilding);
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/edit")
    public R edit(@RequestBody EditBuilding editBuilding) {
        return buildingService.editBuilding(editBuilding);
    }

}

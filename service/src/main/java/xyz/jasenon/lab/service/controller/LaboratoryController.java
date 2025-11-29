package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.laboratory.CreateLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.DeleteLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.EditLaboratory;
import xyz.jasenon.lab.service.service.ILaboratoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@RestController
@RequestMapping("/laboratory")
@CrossOrigin("*")
public class LaboratoryController {

    @Autowired
    private ILaboratoryService laboratoryService;

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PostMapping("/create")
    @Operation(summary = "创建实验室", requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
            examples = @ExampleObject(name = "CreateLaboratory", value = "{\n  \"laboratoryId\": \"LAB-001\",\n  \"laboratoryName\": \"物理实验室\",\n  \"belongToBuilding\": 10,\n  \"area\": 80,\n  \"classCapacity\": 40,\n  \"securityLevel\": \"A级\",\n  \"belongToDeptIds\": [301,302]\n}")))
    public R createLaboratory(@RequestBody CreateLaboratory createLaboratory){
        return laboratoryService.createLaboratory(createLaboratory);
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/edit")
    public R editLaboratory(@RequestBody EditLaboratory editLaboratory){
        return laboratoryService.editLaboratory(editLaboratory);
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @DeleteMapping("/delete")
    public R deleteLaboratory(@RequestBody DeleteLaboratory deleteLaboratory){
        return laboratoryService.deleteLaboratory(deleteLaboratory);
    }

}

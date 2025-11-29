package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.dept.CreateDept;
import xyz.jasenon.lab.service.dto.dept.DeleteDept;
import xyz.jasenon.lab.service.dto.dept.EditDept;
import xyz.jasenon.lab.service.service.IDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@RestController
@RequestMapping("/dept")
@CrossOrigin("*")
public class DeptController {

    @Autowired
    private IDeptService deptService;

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PostMapping("/create")
    @Operation(summary = "创建部门", requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
            examples = @ExampleObject(name = "CreateDept", value = "{\n  \"deptName\": \"理学院\"\n}")))
    public R createDept(@RequestBody CreateDept createDept){
        return deptService.createDept(createDept);
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/edit")
    public R editDept(@RequestBody EditDept editDept){
        return deptService.editDept(editDept);
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @DeleteMapping("/delete")
    public R deleteDept(@RequestBody DeleteDept deleteDept){
        return deptService.deleteDept(deleteDept);
    }

}

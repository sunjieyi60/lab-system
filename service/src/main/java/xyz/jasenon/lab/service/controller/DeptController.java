package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.dept.CreateDept;
import xyz.jasenon.lab.service.dto.dept.DeleteDept;
import xyz.jasenon.lab.service.dto.dept.EditDept;
import xyz.jasenon.lab.service.service.IDeptService;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Api("部门")
@RestController
@RequestMapping("/dept")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class DeptController {

    @Autowired
    private IDeptService deptService;

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PostMapping("/create")
    @ApiOperation("创建部门")
    public R createDept(@Validated @RequestBody CreateDept createDept){
        return deptService.createDept(createDept);
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/edit")
    @ApiOperation("编辑部门")
    public R editDept(@Validated @RequestBody EditDept editDept){
        return deptService.editDept(editDept);
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @DeleteMapping("/delete")
    @ApiOperation("删除部门")
    public R deleteDept(@Validated @RequestBody DeleteDept deleteDept){
        return deptService.deleteDept(deleteDept);
    }

}

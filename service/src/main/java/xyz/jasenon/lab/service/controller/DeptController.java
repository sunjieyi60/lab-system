package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.DiyResponseEntity;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.dept.CreateDept;
import xyz.jasenon.lab.service.dto.dept.DeleteDept;
import xyz.jasenon.lab.service.dto.dept.EditDept;
import xyz.jasenon.lab.service.service.IDeptService;
import xyz.jasenon.lab.common.entity.base.Dept;

import java.util.List;

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
    public DiyResponseEntity<R<Dept>> createDept(@Validated @RequestBody CreateDept createDept){
        return DiyResponseEntity.of(R.success(deptService.createDept(createDept)));
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/edit")
    @ApiOperation("编辑部门")
    public DiyResponseEntity<R<Dept>> editDept(@Validated @RequestBody EditDept editDept){
        return DiyResponseEntity.of(R.success(deptService.editDept(editDept)));
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @DeleteMapping("/delete")
    @ApiOperation("删除部门")
    public DiyResponseEntity<R<Void>> deleteDept(@Validated @RequestBody DeleteDept deleteDept){
        deptService.deleteDept(deleteDept);
        return DiyResponseEntity.of(R.success());
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD, Permissions.BASE_VIEW})
    @GetMapping("/list")
    @ApiOperation("获取部门列表")
    public DiyResponseEntity<R<List<Dept>>> listDept(){
        return DiyResponseEntity.of(R.success(deptService.listDept()));
    }

}

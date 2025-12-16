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

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@RestController
@RequestMapping("/dept")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class DeptController {

    @Autowired
    private IDeptService deptService;

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PostMapping("/create")
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

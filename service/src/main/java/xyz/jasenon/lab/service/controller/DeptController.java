package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.CreateDept;
import xyz.jasenon.lab.service.dto.DeleteDept;
import xyz.jasenon.lab.service.dto.EditDept;
import xyz.jasenon.lab.service.service.IDeptService;

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

    @PostMapping("/create")
    public R createDept(@RequestBody CreateDept createDept){
        return deptService.createDept(createDept);
    }

    @PutMapping("/edit")
    public R editDept(@RequestBody EditDept editDept){
        return deptService.editDept(editDept);
    }

    @DeleteMapping("/delete")
    public R deleteDept(@RequestBody DeleteDept deleteDept){
        return deptService.deleteDept(deleteDept);
    }

}

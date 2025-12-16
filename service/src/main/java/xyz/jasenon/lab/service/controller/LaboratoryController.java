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

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@RestController
@RequestMapping("/laboratory")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class LaboratoryController {

    @Autowired
    private ILaboratoryService laboratoryService;

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PostMapping("/create")
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

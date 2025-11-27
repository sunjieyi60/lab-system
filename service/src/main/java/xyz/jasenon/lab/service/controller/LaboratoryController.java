package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.CreateLaboratory;
import xyz.jasenon.lab.service.dto.DeleteLaboratory;
import xyz.jasenon.lab.service.dto.EditLaboratory;
import xyz.jasenon.lab.service.service.ILaboratoryService;

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

    @PostMapping("/create")
    public R createLaboratory(@RequestBody CreateLaboratory createLaboratory){
        return laboratoryService.createLaboratory(createLaboratory);
    }

    @PutMapping("/edit")
    public R editLaboratory(@RequestBody EditLaboratory editLaboratory){
        return laboratoryService.editLaboratory(editLaboratory);
    }

    @DeleteMapping("/delete")
    public R deleteLaboratory(@RequestBody DeleteLaboratory deleteLaboratory){
        return laboratoryService.deleteLaboratory(deleteLaboratory);
    }

}

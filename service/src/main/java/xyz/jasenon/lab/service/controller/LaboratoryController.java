package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.utils.DiyResponseEntity;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.laboratory.CreateLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.DeleteLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.EditLaboratory;
import xyz.jasenon.lab.service.service.ILaboratoryService;
import xyz.jasenon.lab.service.vo.base.LaboratoryVo;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Api("实验室")
@RestController
@RequestMapping("/laboratory")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class LaboratoryController {

    @Autowired
    private ILaboratoryService laboratoryService;

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PostMapping("/create")
    @ApiOperation("创建实验室")
    public DiyResponseEntity<R<Laboratory>> createLaboratory(@Validated @RequestBody CreateLaboratory createLaboratory){
        return DiyResponseEntity.of(R.success(laboratoryService.createLaboratory(createLaboratory)));
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/edit")
    @ApiOperation("编辑实验室")
    public DiyResponseEntity<R<Laboratory>> editLaboratory(@Validated @RequestBody EditLaboratory editLaboratory){
        return DiyResponseEntity.of(R.success(laboratoryService.editLaboratory(editLaboratory)));
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @DeleteMapping("/delete")
    @ApiOperation("删除实验室")
    public DiyResponseEntity<R<Void>> deleteLaboratory(@Validated @RequestBody DeleteLaboratory deleteLaboratory){
        laboratoryService.deleteLaboratory(deleteLaboratory);
        return DiyResponseEntity.of(R.success());
    }

    @RequestPermission(allowed = {Permissions.BASE_CUD})
    @PutMapping("/editManagers")
    @ApiOperation("编辑实验室管理员")
    public DiyResponseEntity<R<Void>> editManagers(@RequestParam Long laboratoryId, @RequestParam List<Long> userIds){
        laboratoryService.editManagers(laboratoryId, userIds);
        return DiyResponseEntity.of(R.success());
    }

    @GetMapping("/detail")
    @ApiOperation("获取实验室详情")
    public DiyResponseEntity<R<LaboratoryVo>> getLaboratoryDetail(@RequestParam Long laboratoryId){
        return DiyResponseEntity.of(R.success(laboratoryService.getLaboratoryDetailById(laboratoryId)));
    }

}

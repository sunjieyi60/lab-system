package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.service.dto.laboratory.CreateLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.DeleteLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.EditLaboratory;
import xyz.jasenon.lab.service.vo.base.LaboratoryVo;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
public interface ILaboratoryService extends IService<Laboratory> {

    Laboratory createLaboratory(CreateLaboratory createLaboratory);

    Laboratory editLaboratory(EditLaboratory editLaboratory);

    void deleteLaboratory(DeleteLaboratory deleteLaboratory);

    void editManagers(Long laboratoryId, List<Long> userIds);

    LaboratoryVo getLaboratoryDetailById(Long laboratoryId);

}

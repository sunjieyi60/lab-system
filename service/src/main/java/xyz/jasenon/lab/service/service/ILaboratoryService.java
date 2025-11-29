package xyz.jasenon.lab.service.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.laboratory.CreateLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.DeleteLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.EditLaboratory;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
public interface ILaboratoryService extends IService<Laboratory> {

    R createLaboratory(CreateLaboratory createLaboratory);

    R editLaboratory(EditLaboratory editLaboratory);

    R deleteLaboratory(DeleteLaboratory deleteLaboratory);

    List<Laboratory> listAll();

}

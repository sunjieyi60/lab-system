package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.Building;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.DeleteBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
public interface IBuildingService extends IService<Building> {

    R createBuilding(CreateBuilding createBuilding);

    R editBuilding(EditBuilding editBuilding);

    R deleteBuilding(DeleteBuilding deleteBuilding);
}

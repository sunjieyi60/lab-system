package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.Building;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.DeleteBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
public interface IBuildingService extends IService<Building> {

    Building createBuilding(CreateBuilding createBuilding);

    Building editBuilding(EditBuilding editBuilding);

    void deleteBuilding(DeleteBuilding deleteBuilding);

    /**
     * 获取楼栋列表
     */
    List<Building> listBuilding();
}

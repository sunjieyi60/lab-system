package xyz.jasenon.lab.common.entity.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

@Getter
@Setter
@Accessors(chain = true)
public class Building extends BaseEntity {

    /**
     * 名称
     */
    private String buildingName;

}

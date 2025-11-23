package xyz.jasenon.lab.common.entity.base;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.common.entity.BaseEntity;

@Getter
@Setter
public class LaboratoryContactUser extends BaseEntity {

    /**
     * 实验室编号
     */
    private Long laboratoryId;

    /**
     * 用户ID
     */
    private Long userId;

}

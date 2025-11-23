package xyz.jasenon.lab.common.entity.device;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

@Getter
@Setter
@Accessors(chain = true)
public class BaseSocketEntity extends BaseEntity {

    /**
     * mac地址
     */
    private String mac;

    /**
     * ip地址
     */
    private String ip;

    /**
     * 所属实验室id
     */
    private Long belongToLaboratoryId;

}

package xyz.jasenon.lab.common.entity.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;

/**
 * @author Jasenon_ce
 */
@Getter
@Setter
@Accessors(chain = true)
public class DeptUser extends BaseEntity {

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 用户ID
     */
    private Long userId;

}

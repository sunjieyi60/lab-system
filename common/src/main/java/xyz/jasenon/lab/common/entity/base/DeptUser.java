package xyz.jasenon.lab.common.entity.base;

import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("dept_user")
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

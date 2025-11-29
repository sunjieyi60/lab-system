package xyz.jasenon.lab.service.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;
import xyz.jasenon.lab.service.constants.Permissions;
import com.baomidou.mybatisplus.annotation.TableName;

@Getter
@Setter
@Accessors(chain = true)
@TableName("user_permission")
public class UserPermission extends BaseEntity {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 权限
     */
    private Permissions permission;
}

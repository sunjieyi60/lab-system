package xyz.jasenon.lab.service.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;
import xyz.jasenon.lab.service.constants.Permissions;

@Getter
@Setter
@Accessors(fluent = true)
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

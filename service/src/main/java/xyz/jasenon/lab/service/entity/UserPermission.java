package xyz.jasenon.lab.service.entity;

import xyz.jasenon.lab.common.entity.BaseEntity;
import xyz.jasenon.lab.service.constants.Permissions;

public class UserPermission extends BaseEntity {

    private Long userId;

    private Permissions permission;
}

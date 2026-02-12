package xyz.jasenon.lab.service.vo.base;

import lombok.Getter;
import lombok.Setter;
import xyz.jasenon.lab.service.constants.Permissions;

import java.util.List;

@Getter
@Setter
public class UserPermissionVo {

    /**
     * 权限
     */
    private Permissions permission;

    /**
     * 权限路径
     */
    private List<Integer> path;
}

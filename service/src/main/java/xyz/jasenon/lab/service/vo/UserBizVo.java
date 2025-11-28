package xyz.jasenon.lab.service.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.base.Dept;
import xyz.jasenon.lab.common.entity.base.Laboratory;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/28
 */
@Getter
@Setter
@Accessors(fluent = true)
public class UserBizVo {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 权限组
     */
    private List<UserPermissionVo> permissions;

    /**
     * 所属部门
     */
    private List<Dept> depts;

    /**
     * 用户所拥有的实验室
     */
    private List<Laboratory> laboratories;

}

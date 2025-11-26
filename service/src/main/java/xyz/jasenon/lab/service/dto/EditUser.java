package xyz.jasenon.lab.service.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.service.constants.Permissions;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Getter
@Setter
@Accessors(fluent = true)
public class EditUser {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 密码
     */
    private String password;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 权限列表
     */
    private List<Permissions> permissions;

    /**
     * 部门ID列表
     */
    private List<Long> deptIds;

    /**
     * 实验室ID列表
     */
    private List<Long> laboratoryIds;

}

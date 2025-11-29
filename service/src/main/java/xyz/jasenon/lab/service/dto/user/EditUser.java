package xyz.jasenon.lab.service.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
@Accessors(chain = true)
public class EditUser {

    /**
     * 用户ID
     */
    @NotNull
    private Long userId;

    /**
     * 密码
     */
    @Pattern(regexp = "^[a-zA-Z0-9]{6,18}$", message = "密码必须是6到18位，允许字母数字下划线")
    private String password;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Pattern(regexp = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$", message = "邮箱格式不正确")
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

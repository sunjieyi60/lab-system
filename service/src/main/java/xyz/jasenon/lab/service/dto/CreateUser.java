package xyz.jasenon.lab.service.dto;

import jakarta.validation.constraints.NotBlank;
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
@Accessors(fluent = true)
public class CreateUser {

    /**
     * 用户名
     */
    @Pattern(regexp = "^[a-zA-Z0-9_-]{4,16}$", message = "用户名必须是4到16位，允许字母数字下划线")
    @NotBlank
    private String username;

    /**
     * 密码
     */
    @Pattern(regexp = "^[a-zA-Z0-9]{6,18}$", message = "密码必须是6到18位，允许字母数字下划线")
    @NotBlank
    private String password;

    /**
     * 真实姓名
     */
    @NotBlank
    private String realName;

    /**
     * 邮箱地址
     */
    @Pattern(regexp = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$", message = "邮箱格式不正确")
    @NotBlank
    private String email;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @NotBlank
    private String phone;

    /**
     * 创建者id
     */
    @NotNull
    private Long createBy;

    /**
     * 权限列表
     */
    private List<Permissions> permissions;

    /**
     * 部门列表
     */
    private List<Long> deptIds;

    /**
     * 实验室列表
     */
    private List<Long> laboratoryIds;

}

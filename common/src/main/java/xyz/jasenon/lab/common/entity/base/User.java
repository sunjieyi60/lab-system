package xyz.jasenon.lab.common.entity.base;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xyz.jasenon.lab.common.entity.BaseEntity;
import xyz.jasenon.lab.common.entity.handler.Md5Encrypt;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * @author Jasenon_ce
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    @TableField(typeHandler = Md5Encrypt.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 创建者ID
     */
    private Long createBy;

    /**
     *  路径枚举
     */
    private String path;

    @TableLogic
    private Boolean deleted;


}

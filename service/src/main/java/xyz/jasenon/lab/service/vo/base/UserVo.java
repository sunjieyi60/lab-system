package xyz.jasenon.lab.service.vo.base;

import lombok.Data;

@Data
public class UserVo {

    /**
     * id
     */
    private Long id;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

}

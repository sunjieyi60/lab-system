package xyz.jasenon.lab.service.log;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.common.entity.base.User;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.dto.user.DeleteUser;
import xyz.jasenon.lab.service.dto.user.EditUser;
import xyz.jasenon.lab.service.service.IUserService;

/**
 * 账号管理操作日志解释器：添加/修改/删除用户等 content。
 */
@Component
@RequiredArgsConstructor
public class UserLogInterpreter {

    private final IUserService userService;

    public Object renderCreate(CreateUser payload) {
        String name = payload.getRealName() != null ? payload.getRealName() : payload.getUsername();
        return "添加用户 " + name + " (" + payload.getUsername() + ")";
    }

    public Object renderEdit(EditUser payload) {
        String name = payload.getRealName();
        if (name == null && payload.getUserId() != null) {
            User u = userService.getById(payload.getUserId());
            name = u != null ? u.getRealName() : null;
        }
        return "修改用户 " + (name != null ? name : "ID " + payload.getUserId());
    }

    public Object renderDelete(DeleteUser payload) {
        User u = userService.getById(payload.getUserId());
        String name = u != null ? (u.getRealName() != null ? u.getRealName() : u.getUsername()) : null;
        return "删除用户 " + (name != null ? name : "ID " + payload.getUserId());
    }
}

package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.User;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.dto.user.DeleteUser;
import xyz.jasenon.lab.service.dto.user.EditUser;
import xyz.jasenon.lab.service.dto.user.UserLogin;
import xyz.jasenon.lab.service.vo.base.UserBizVo;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
public interface IUserService extends IService<User> {

    User createUser(CreateUser createUser);

    User editUser(EditUser editUser);

    void deleteUser(DeleteUser deleteUser);

    UserBizVo getCurrentUserDetail();

    void login(UserLogin userLogin);

    void logout();

    List<UserBizVo> visibleTreeVo();

    Permissions.PermissionTree permissionTree();

    List<User> visible();

}

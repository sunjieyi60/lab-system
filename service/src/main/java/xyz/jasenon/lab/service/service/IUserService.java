package xyz.jasenon.lab.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xyz.jasenon.lab.common.entity.base.User;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.CreateUser;
import xyz.jasenon.lab.service.dto.DeleteUser;
import xyz.jasenon.lab.service.dto.EditUser;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
public interface IUserService extends IService<User> {

    R createUser(CreateUser createUser);

    R editUser(EditUser editUser);

    R deleteUser(DeleteUser deleteUser);

    List<User> visible();

}

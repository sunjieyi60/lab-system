package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.dto.user.DeleteUser;
import xyz.jasenon.lab.service.dto.user.EditUser;
import xyz.jasenon.lab.service.service.IUserService;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@RestController
@RequestMapping("/user")
@CrossOrigin("*")
public class UserController {

    @Autowired
    private IUserService userService;

    @PostMapping("/create")
    public R createUser(CreateUser createUser){
        return userService.createUser(createUser);
    }

    @PutMapping("/edit")
    public R editUser(EditUser editUser){
        return userService.editUser(editUser);
    }

    @DeleteMapping("/delete")
    public R deleteUser(DeleteUser deleteUser){
        return userService.deleteUser(deleteUser);
    }

    @GetMapping("/getCurrentUserDetail")
    public R getCurrentUserDetail(){
        return userService.getCurrentUserDetail();
    }
}

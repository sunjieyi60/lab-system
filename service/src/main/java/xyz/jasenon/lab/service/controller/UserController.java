package xyz.jasenon.lab.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.dto.user.DeleteUser;
import xyz.jasenon.lab.service.dto.user.EditUser;
import xyz.jasenon.lab.service.dto.user.UserLogin;
import xyz.jasenon.lab.service.service.IUserService;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class UserController {

    @Autowired
    private IUserService userService;

    @RequestPermission(allowed = {Permissions.USER_ADD})
    @PostMapping("/create")
    public R createUser(@RequestBody CreateUser createUser){
        return userService.createUser(createUser);
    }

    @RequestPermission(allowed = {Permissions.USER_EDIT})
    @PutMapping("/edit")
    public R editUser(@RequestBody EditUser editUser){
        return userService.editUser(editUser);
    }

    @RequestPermission(allowed = {Permissions.USER_DELETE})
    @DeleteMapping("/delete")
    public R deleteUser(@RequestBody DeleteUser deleteUser){
        return userService.deleteUser(deleteUser);
    }

    @PostMapping("/login")
    public R login(@RequestBody UserLogin userLogin){
        return userService.login(userLogin);
    }

    @GetMapping("/logout")
    public R logout(){
        return userService.logout();
    }

    @GetMapping("/getCurrentUserDetail")
    public R getCurrentUserDetail(){
        return userService.getCurrentUserDetail();
    }
}

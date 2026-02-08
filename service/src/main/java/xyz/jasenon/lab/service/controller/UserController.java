package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.annotation.log.LogPoint;
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
@Api("用户")
@RestController
@RequestMapping("/user")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class UserController {

    @Autowired
    private IUserService userService;

    @RequestPermission(allowed = {Permissions.USER_ADD})
    @PostMapping("/create")
    @ApiOperation("创建用户")
    @LogPoint(title = "'账号管理'", sqEl = "#createUser", clazz = CreateUser.class)
    public R createUser(@Validated @RequestBody CreateUser createUser){
        return userService.createUser(createUser);
    }

    @RequestPermission(allowed = {Permissions.USER_EDIT})
    @PutMapping("/edit")
    @ApiOperation("编辑用户")
    @LogPoint(title = "'账号管理'", sqEl = "#editUser", clazz = EditUser.class)
    public R editUser(@Validated @RequestBody EditUser editUser){
        return userService.editUser(editUser);
    }

    @RequestPermission(allowed = {Permissions.USER_DELETE})
    @DeleteMapping("/delete")
    @ApiOperation("删除用户")
    @LogPoint(title = "'账号管理'", sqEl = "#deleteUser", clazz = DeleteUser.class)
    public R deleteUser(@Validated @RequestBody DeleteUser deleteUser){
        return userService.deleteUser(deleteUser);
    }

    @PostMapping("/login")
    @ApiOperation("登录")
    public R login(@Validated @RequestBody UserLogin userLogin){
        return userService.login(userLogin);
    }

    @GetMapping("/logout")
    @ApiOperation("退出登录")
    public R logout(){
        return userService.logout();
    }

    @GetMapping("/getCurrentUserDetail")
    @ApiOperation("获取用户详细信息")
    public R getCurrentUserDetail(){
        return userService.getCurrentUserDetail();
    }
}

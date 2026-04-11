package xyz.jasenon.lab.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import xyz.jasenon.lab.common.utils.DiyResponseEntity;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.annotation.log.LogPoint;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.dto.user.DeleteUser;
import xyz.jasenon.lab.service.dto.user.EditUser;
import xyz.jasenon.lab.service.dto.user.UserLogin;
import xyz.jasenon.lab.service.service.IUserService;
import xyz.jasenon.lab.common.entity.base.User;
import xyz.jasenon.lab.service.vo.base.UserBizVo;

import java.util.List;

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
    public DiyResponseEntity<R<User>> createUser(@Validated @RequestBody CreateUser createUser){
        return DiyResponseEntity.of(R.success(userService.createUser(createUser)));
    }

    @RequestPermission(allowed = {Permissions.USER_EDIT})
    @PutMapping("/edit")
    @ApiOperation("编辑用户")
    @LogPoint(title = "'账号管理'", sqEl = "#editUser", clazz = EditUser.class)
    public DiyResponseEntity<R<User>> editUser(@Validated @RequestBody EditUser editUser){
        return DiyResponseEntity.of(R.success(userService.editUser(editUser)));
    }

    @RequestPermission(allowed = {Permissions.USER_DELETE})
    @DeleteMapping("/delete")
    @ApiOperation("删除用户")
    @LogPoint(title = "'账号管理'", sqEl = "#deleteUser", clazz = DeleteUser.class)
    public DiyResponseEntity<R<Void>> deleteUser(@Validated @RequestBody DeleteUser deleteUser){
        userService.deleteUser(deleteUser);
        return DiyResponseEntity.of(R.success());
    }

    @PostMapping("/login")
    @ApiOperation("登录")
    public DiyResponseEntity<R<Void>> login(@Validated @RequestBody UserLogin userLogin){
        userService.login(userLogin);
        return DiyResponseEntity.of(R.success());
    }

    @GetMapping("/logout")
    @ApiOperation("退出登录")
    public DiyResponseEntity<R<Void>> logout(){
        userService.logout();
        return DiyResponseEntity.of(R.success());
    }

    @GetMapping("/getCurrentUserDetail")
    @ApiOperation("获取用户详细信息")
    public DiyResponseEntity<R<UserBizVo>> getCurrentUserDetail(){
        return DiyResponseEntity.of(R.success(userService.getCurrentUserDetail()));
    }

    @GetMapping("/getVisibleTree")
    @ApiOperation("获取所有当前用户可见的用户")
    public DiyResponseEntity<R<List<UserBizVo>>> getVisibleTree(){
        return DiyResponseEntity.of(R.success(userService.visibleTreeVo()));
    }

    @GetMapping("/permission_tree")
    @ApiOperation("获取权限树")
    public DiyResponseEntity<R<Permissions.PermissionTree>> permissionTree() {
        return DiyResponseEntity.of(R.success(userService.permissionTree()));
    }
}

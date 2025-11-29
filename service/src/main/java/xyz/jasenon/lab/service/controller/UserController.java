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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

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

    @RequestPermission(allowed = {Permissions.USER_ADD})
    @PostMapping("/create")
    @Operation(summary = "创建用户", requestBody = @RequestBody(required = true, content = @Content(mediaType = "application/json",
            examples = @ExampleObject(name = "CreateUser", value = "{\n  \"username\": \"system\",\n  \"password\": \"123456\",\n  \"realName\": \"系统\",\n  \"email\": \"system@example.com\",\n  \"phone\": \"13900000000\",\n  \"createBy\": 1,\n  \"permissions\": [\n    \"USER_ADD\",\n    \"USER_EDIT\",\n    \"USER_DELETE\",\n    \"SCHEDULE_CLASSES\",\n    \"SCHEDULE_CLASSES_VIEW\",\n    \"SEMESTER_SETTINGS\",\n    \"DEVICE_ADD\",\n    \"DEVICE_CONTROL\",\n    \"DEVICE_SMART_CONTROL\",\n    \"DEVICE_ALARM_SETTINGS\",\n    \"ACADEMIC_AFFAIRS_ANALYSIS\",\n    \"LABORATORY_POWER_CONSUMPTION\",\n    \"LABORATORY_CENTRAL_AIRCONDITION\",\n    \"BASE_CUD\",\n    \"BASE_VIEW\"\n  ],\n  \"deptIds\": [1],\n  \"laboratoryIds\": [101]\n}"))))
    public R createUser(CreateUser createUser){
        return userService.createUser(createUser);
    }

    @RequestPermission(allowed = {Permissions.USER_EDIT})
    @PutMapping("/edit")
    public R editUser(EditUser editUser){
        return userService.editUser(editUser);
    }

    @RequestPermission(allowed = {Permissions.USER_DELETE})
    @DeleteMapping("/delete")
    public R deleteUser(DeleteUser deleteUser){
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

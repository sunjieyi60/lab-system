package xyz.jasenon.lab.service.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import xyz.jasenon.lab.service.annotation.RequestPermission;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.entity.UserPermission;
import xyz.jasenon.lab.service.exception.PermissionsInsufficientException;
import xyz.jasenon.lab.service.mapper.UserPermissionMapper;

import java.util.Arrays;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/29
 */
@Aspect
@Component
@Slf4j
public class PermissionCheck {

    @Autowired
    private UserPermissionMapper userPermissionMapper;

    @Pointcut("@annotation(xyz.jasenon.lab.service.annotation.RequestPermission)")
    public void permissionCheck() {};

    @Before("permissionCheck()")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        RequestPermission $ = methodSignature.getMethod().getAnnotation(RequestPermission.class);
        if ($.allowed().length == 0) {
            return;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        List<Permissions> permissions = userPermissionMapper.selectList(
                new LambdaQueryWrapper<UserPermission>().eq(UserPermission::getUserId, userId)
        ).stream().map(UserPermission::getPermission).toList();
        boolean isOver = permissions.stream().noneMatch(Arrays.asList($.allowed())::contains);
        if (isOver) {
            throw new PermissionsInsufficientException("需要权限"+ Arrays.toString($.allowed()));
        }
    }

}

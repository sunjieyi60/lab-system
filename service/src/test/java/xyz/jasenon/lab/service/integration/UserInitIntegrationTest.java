package xyz.jasenon.lab.service.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import xyz.jasenon.lab.common.entity.base.User;
import xyz.jasenon.lab.service.ServiceApplication;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.entity.UserPermission;
import xyz.jasenon.lab.service.mapper.UserMapper;
import xyz.jasenon.lab.service.mapper.UserPermissionMapper;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ServiceApplication.class)
class UserInitIntegrationTest {

    // 直接使用 Mapper 执行插入初始化

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserPermissionMapper userPermissionMapper;

    @Test
    void initSystemUser_withAllLeafPermissions_insertsUsingMappers() {
        // 计算叶子权限（没有任何子节点的权限）
        List<Permissions> leafPermissions = Arrays.stream(Permissions.values())
                .filter(p -> Arrays.stream(Permissions.values())
                        .noneMatch(x -> x.getParentId().equals(p.getId())))
                .toList();

        // 直接插入用户
        User user = new User()
                .setUsername("system")
                .setPassword("123456")
                .setRealName("系统")
                .setEmail("system@example.com")
                .setPhone("13900000000");
        userMapper.insert(user);
        assertNotNull(user.getId());

        // 直接插入权限映射（仅叶子权限）
        for (Permissions p : leafPermissions) {
            UserPermission up = new UserPermission()
                    .setUserId(user.getId())
                    .setPermission(p);
            userPermissionMapper.insert(up);
        }

        // 验证数据库已插入该用户
        User saved = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "system"));
        assertNotNull(saved);
    }
}
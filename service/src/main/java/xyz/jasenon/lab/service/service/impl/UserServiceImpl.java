package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jasenon.lab.common.entity.base.Dept;
import xyz.jasenon.lab.common.entity.base.DeptUser;
import xyz.jasenon.lab.common.entity.base.LaboratoryUser;
import xyz.jasenon.lab.common.entity.base.User;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.CreateUser;
import xyz.jasenon.lab.service.entity.UserPermission;
import xyz.jasenon.lab.service.mapper.*;
import xyz.jasenon.lab.service.service.IUserService;

import java.util.HashSet;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private DeptUserMapper deptUserMapper;
    @Autowired
    private LaboratoryUserMapper laboratoryMapper;
    @Autowired
    private UserPermissionMapper userPermissionMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public R createUser(CreateUser createUser) {

        String username = createUser.username();
        String password = createUser.password();
        String realName = createUser.realName();
        String email = createUser.email();
        String phone = createUser.phone();
        Long createBy = StpUtil.getLoginIdAsLong();

        User target = baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (target != null) {
            return R.fail("用户已存在");
        }

        User user = new User()
                .setUsername(username)
                .setPassword(password)
                .setRealName(realName)
                .setEmail(email)
                .setPhone(phone)
                .setCreateBy(createBy);
        this.save(user);

        // 分配权限
        List<Permissions> permissions = createUser.permissions();
        if (!permissions.isEmpty()){
            List<Permissions> doUserPermissions = new LambdaQueryChainWrapper<>(userPermissionMapper)
                    .eq(UserPermission::userId, createBy)
                    .list()
                    .stream().map(UserPermission::permission).toList();

            // 是否越权，当前用户只能将自己拥有的权限赋予给其他用户
            boolean pmOver = new HashSet<>(doUserPermissions).containsAll(permissions);
            if (!pmOver){
                return R.fail("权限越权");
            }
            for (Permissions permission : permissions){
                UserPermission userPermission = new UserPermission()
                        .permission(permission)
                        .userId(user.getId());
                userPermissionMapper.insert(userPermission);
            }
        }

        // 分配部门
        List<Long> deptIds = createUser.deptIds();
        if (!deptIds.isEmpty()){
            for (Long deptId : deptIds){
                Dept dept = deptMapper.selectById(deptId);
                if (dept == null) {
                    return R.fail("部门不存在");
                }
                DeptUser deptUser = new DeptUser()
                        .setUserId(user.getId())
                        .setDeptId(dept.getId());
                deptUserMapper.insert(deptUser);
            }
        }

        // 分配实验室
        List<Long> laboratoryIds = createUser.laboratoryIds();
        if (!laboratoryIds.isEmpty()){
            List<Long> doLaboratoryIds = new LambdaQueryChainWrapper<>(laboratoryMapper)
                    .eq(LaboratoryUser::userId, createBy)
                    .list().stream().map(LaboratoryUser::laboratoryId).toList();
            boolean labOver = new HashSet<>(doLaboratoryIds).containsAll(laboratoryIds);
            if (!labOver){
                return R.fail("实验室越权");
            }
            for (Long laboratoryId : laboratoryIds){
                LaboratoryUser laboratoryUser = new LaboratoryUser()
                        .userId(user.getId())
                        .laboratoryId(laboratoryId);
                laboratoryMapper.insert(laboratoryUser);
            }
        }
        return R.success("新建用户成功");
    }
}

package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.query.MPJLambdaQueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jasenon.lab.common.entity.base.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.dto.user.DeleteUser;
import xyz.jasenon.lab.service.dto.user.EditUser;
import xyz.jasenon.lab.service.dto.user.UserLogin;
import xyz.jasenon.lab.service.entity.UserPermission;
import xyz.jasenon.lab.service.mapper.*;
import xyz.jasenon.lab.service.service.IUserService;
import xyz.jasenon.lab.service.vo.UserBizVo;
import xyz.jasenon.lab.service.vo.UserPermissionVo;

import java.util.ArrayList;
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
    private LaboratoryUserMapper laboratoryUserMapper;
    @Autowired
    private UserPermissionMapper userPermissionMapper;
    @Autowired
    private LaboratoryMapper laboratoryMapper;

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
            List<Long> doDeptIds = new LambdaQueryChainWrapper<>(deptUserMapper)
                    .eq(DeptUser::getUserId, createBy)
                    .list().stream().map(DeptUser::getDeptId).toList();
            boolean deptOver = new HashSet<>(doDeptIds).containsAll(deptIds);
            if (!deptOver){
                return R.fail("部门越权");
            }
            for (Long deptId : deptIds){
                DeptUser deptUser = new DeptUser()
                        .setUserId(user.getId())
                        .setDeptId(deptId);
                deptUserMapper.insert(deptUser);
            }
        }

        // 分配实验室
        List<Long> laboratoryIds = createUser.laboratoryIds();
        if (!laboratoryIds.isEmpty()){
            List<Long> doLaboratoryIds = new LambdaQueryChainWrapper<>(laboratoryUserMapper)
                    .eq(LaboratoryUser::getUserId, createBy)
                    .list().stream().map(LaboratoryUser::getLaboratoryId).toList();
            boolean labOver = new HashSet<>(doLaboratoryIds).containsAll(laboratoryIds);
            if (!labOver){
                return R.fail("实验室越权");
            }
            for (Long laboratoryId : laboratoryIds){
                LaboratoryUser laboratoryUser = new LaboratoryUser()
                        .setUserId(user.getId())
                        .setLaboratoryId(laboratoryId);
                laboratoryUserMapper.insert(laboratoryUser);
            }
        }
        return R.success("新建用户成功");
    }

    @Override
    public R editUser(EditUser editUser) {

        Long doUserId = StpUtil.getLoginIdAsLong();
        List<User> visible = visible();
        boolean isVisible = visible.stream().anyMatch(user -> user.getId().equals(editUser.userId()));

        if (!isVisible){
            return R.fail("无权编辑该用户");
        }

        User user = baseMapper.selectById(editUser.userId());
        if (user == null) {
            return R.fail("用户不存在");
        }
        User edit = new User()
                .setPassword(editUser.password())
                .setPhone(editUser.phone())
                .setRealName(editUser.realName())
                .setEmail(editUser.email());

        CopyOptions copyOptions = CopyOptions.create()
                .setIgnoreProperties("id", "createTime")
                .ignoreNullValue();

        BeanUtil.copyProperties(edit, user, copyOptions);
        this.updateById(user);

        deptUserMapper.delete(new LambdaQueryWrapper<DeptUser>()
                .eq(DeptUser::getUserId,user.getId()));
        laboratoryUserMapper.delete(new LambdaQueryWrapper<LaboratoryUser>()
                .eq(LaboratoryUser::getUserId,user.getId()));
        userPermissionMapper.delete(new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::userId,user.getId()));

        // 分配权限
        List<Permissions> permissions = editUser.permissions();
        if (!permissions.isEmpty()){
            List<Permissions> doUserPermissions = new LambdaQueryChainWrapper<>(userPermissionMapper)
                    .eq(UserPermission::userId, doUserId)
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
        List<Long> deptIds = editUser.deptIds();
        if (!deptIds.isEmpty()){
            List<Long> doDeptIds = new LambdaQueryChainWrapper<>(deptUserMapper)
                    .eq(DeptUser::getUserId, doUserId)
                    .list().stream().map(DeptUser::getDeptId).toList();
            boolean deptOver = new HashSet<>(doDeptIds).containsAll(deptIds);
            if (!deptOver){
                return R.fail("部门越权");
            }
            for (Long deptId : deptIds){
                DeptUser deptUser = new DeptUser()
                        .setUserId(user.getId())
                        .setDeptId(deptId);
                deptUserMapper.insert(deptUser);
            }
        }

        // 分配实验室
        List<Long> laboratoryIds = editUser.laboratoryIds();
        if (!laboratoryIds.isEmpty()){
            List<Long> doLaboratoryIds = new LambdaQueryChainWrapper<>(laboratoryUserMapper)
                    .eq(LaboratoryUser::getUserId, doUserId)
                    .list().stream().map(LaboratoryUser::getLaboratoryId).toList();
            boolean labOver = new HashSet<>(doLaboratoryIds).containsAll(laboratoryIds);
            if (!labOver){
                return R.fail("实验室越权");
            }
            for (Long laboratoryId : laboratoryIds){
                LaboratoryUser laboratoryUser = new LaboratoryUser()
                        .setUserId(user.getId())
                        .setLaboratoryId(laboratoryId);
                laboratoryUserMapper.insert(laboratoryUser);
            }
        }

        return R.success("编辑用户成功");
    }

    @Override
    public R deleteUser(DeleteUser deleteUser) {
        Long doUserId = StpUtil.getLoginIdAsLong();
        List<User> visible = visible();
        boolean isVisible = visible.stream().anyMatch(user -> user.getId().equals(deleteUser.userId()));
        if (!isVisible){
            return R.fail("无权删除该用户");
        }
        User user = baseMapper.selectById(deleteUser.userId());
        if (user == null) {
            return R.fail("用户不存在");
        }
        this.removeById(user.getId());
        return R.success("删除用户成功");
    }

    @Override
    public R getCurrentUserDetail() {
        Long doUserId = StpUtil.getLoginIdAsLong();
        User user = baseMapper.selectById(doUserId);
        if (user == null) {
            return R.fail("用户不存在");
        }
        UserBizVo userBizVo = userToUserBizVo(user);
        return R.success(userBizVo,"获取当前用户详情成功");
    }

    @Override
    public R login(UserLogin userLogin) {
        User user = baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, userLogin.getUsername())
        );
        if (user == null) {
            return R.fail("用户不存在");
        }
        if (!user.getPassword().equals(MD5.create()
                .digestHex(userLogin.getPassword()))){
            return R.fail("密码错误");
        }
        StpUtil.login(user.getId());
        return R.success("登录成功");
    }

    @Override
    public R logout() {
        StpUtil.logout();
        return R.success("登出成功");
    }

    @Override
    public R visibleTreeVo() {
        Long doUserId = StpUtil.getLoginIdAsLong();
        List<User> visible = visible();
        List<UserBizVo> userBizVos = visible.stream().map(this::userToUserBizVo).toList();
        return R.success(userBizVos,"获取可见用户树成功");
    }

    private UserBizVo userToUserBizVo(User user){
        UserBizVo userBizVo = BeanUtil.copyProperties(user, UserBizVo.class);
        List<Dept> depts = deptMapper.selectJoinList(
                new MPJLambdaWrapper<Dept>()
                        .select(Dept::getId, Dept::getDeptName)
                        .leftJoin(DeptUser.class,on -> {
                            on.eq(DeptUser::getUserId, user.getId());
                            on.eq(DeptUser::getDeptId,Dept::getId);
                            return on;
                        })
        );
        List<Laboratory> laboratories = laboratoryMapper.selectJoinList(
                new MPJLambdaWrapper<Laboratory>()
                        .selectAll(Laboratory.class)
                        .leftJoin(LaboratoryUser.class,on -> {
                            on.eq(LaboratoryUser::getUserId, user.getId());
                            on.eq(LaboratoryUser::getLaboratoryId,Laboratory::getId);
                            return on;
                        })
        );
        List<UserPermission> userPermissions = userPermissionMapper.selectList(
                new LambdaQueryWrapper<UserPermission>().eq(UserPermission::userId,user.getId())
        );
        List<UserPermissionVo> vos = userPermissions.stream().map(userPermission -> {
            UserPermissionVo vo = new UserPermissionVo();
            vo.setPermission(userPermission.permission());
            vo.setPath(Permissions.pathOf(userPermission.permission()));
            return vo;
        }).toList();
        userBizVo.depts(depts);
        userBizVo.laboratories(laboratories);
        userBizVo.permissions(vos);
        return userBizVo;
    }

    @Override
    public List<User> visible() {
        Long doUserId = StpUtil.getLoginIdAsLong();
        User self = baseMapper.selectById(doUserId);
        List<User> users = this.lambdaQuery().eq(User::getCreateBy, doUserId).list();
        List<User> res = dfs(users, new ArrayList<>());
        res.add(self);
        return res;
    }

    private List<User> dfs(List<User> users, List<User> res){
        if (users.isEmpty()){
            return res;
        }
        res.addAll(users);
        for (User user : users){
            List<User> children = this.lambdaQuery().eq(User::getCreateBy, user.getId()).list();
            if(!children.isEmpty()){
                return dfs(children, res);
            }
        }
        return res;
    }
}

package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import xyz.jasenon.lab.service.vo.base.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    @Autowired
    private LaboratoryManagerMapper laboratoryManagerMapper;
    @Autowired
    private BuildingMapper buildingMapper;
    @Autowired
    private DeptBuildingMapper deptBuildingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R createUser(CreateUser createUser) {

        String username = createUser.getUsername();
        String password = createUser.getPassword();
        String realName = createUser.getRealName();
        String email = createUser.getEmail();
        String phone = createUser.getPhone();
        Long createBy = StpUtil.getLoginIdAsLong();

        User target = baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (target != null) {
            return R.fail("用户已存在");
        }

        User doUser = baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, createBy));
        String doUserPath = doUser.getPath();

        User user = new User()
                .setUsername(username)
                .setPassword(password)
                .setRealName(realName)
                .setEmail(email)
                .setPhone(phone)
                .setCreateBy(createBy);
        this.save(user);
        
        // 设置用户的path：父用户的path + 当前用户ID + '/'（包含自身）
        String userPath;
        if (doUserPath == null || doUserPath.isEmpty()) {
            // 如果父用户没有path，说明是根用户，设置为 /用户ID/
            userPath = "/" + user.getId() + "/";
        } else {
            // 父用户的path + 当前用户ID + '/'
            userPath = doUserPath + user.getId() + "/";
        }
        user.setPath(userPath);
        this.updateById(user);

        // 分配权限
        List<Permissions> permissions = createUser.getPermissions();
        if (!permissions.isEmpty()){
            List<Permissions> doUserPermissions = new LambdaQueryChainWrapper<>(userPermissionMapper)
                    .eq(UserPermission::getUserId, createBy)
                    .list()
                    .stream().map(UserPermission::getPermission).toList();

            // 是否越权，当前用户只能将自己拥有的权限赋予给其他用户
            boolean pmOver = new HashSet<>(doUserPermissions).containsAll(permissions);
            if (!pmOver){
                return R.fail("权限越权");
            }
            for (Permissions permission : permissions){
                UserPermission userPermission = new UserPermission()
                        .setPermission(permission)
                        .setUserId(user.getId());
                userPermissionMapper.insert(userPermission);
            }
        }

        // 分配部门
        List<Long> deptIds = createUser.getDeptIds();
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
        List<Long> laboratoryIds = createUser.getLaboratoryIds();
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
        boolean isVisible = visible.stream().anyMatch(user -> user.getId().equals(editUser.getUserId()));

        if (!isVisible){
            return R.fail("无权编辑该用户");
        }

        User user = baseMapper.selectById(editUser.getUserId());
        if (user == null) {
            return R.fail("用户不存在");
        }
        User edit = new User()
                .setPassword(editUser.getPassword())
                .setPhone(editUser.getPhone())
                .setRealName(editUser.getRealName())
                .setEmail(editUser.getEmail());

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
                .eq(UserPermission::getUserId, user.getId()));

        // 分配权限
        List<Permissions> permissions = editUser.getPermissions();
        if (!permissions.isEmpty()){
            List<Permissions> doUserPermissions = new LambdaQueryChainWrapper<>(userPermissionMapper)
                    .eq(UserPermission::getUserId, doUserId)
                    .list()
                    .stream().map(UserPermission::getPermission).toList();

            // 是否越权，当前用户只能将自己拥有的权限赋予给其他用户
            boolean pmOver = new HashSet<>(doUserPermissions).containsAll(permissions);
            if (!pmOver){
                return R.fail("权限越权");
            }
            for (Permissions permission : permissions){
                UserPermission userPermission = new UserPermission()
                        .setPermission(permission)
                        .setUserId(user.getId());
                userPermissionMapper.insert(userPermission);
            }
        }

        // 分配部门
        List<Long> deptIds = editUser.getDeptIds();
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
        List<Long> laboratoryIds = editUser.getLaboratoryIds();
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
        boolean isVisible = visible.stream().anyMatch(user -> user.getId().equals(deleteUser.getUserId()));
        if (!isVisible){
            return R.fail("无权删除该用户");
        }
        User user = baseMapper.selectById(deleteUser.getUserId());
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

    @Override
    public R permissionTree() {
        return R.success(Permissions.tree, "获取权限树成功");
    }

    private UserBizVo userToUserBizVo(User user){
        UserBizVo userBizVo = BeanUtil.copyProperties(user, UserBizVo.class);
        List<Dept> depts = deptUserMapper.selectJoinList(Dept.class,
            new MPJLambdaWrapper<DeptUser>()
                    .selectAll(Dept.class)
                    .leftJoin(Dept.class, on->on.eq(DeptUser::getUserId, user.getId())
                            .eq(DeptUser::getDeptId, Dept::getId))
        );
        List<DeptVo> deptVos = depts.stream()
                .filter(dept -> dept != null)
                .map(dept -> {
            DeptVo vo = new DeptVo();
            List<Building> buildings = deptBuildingMapper.selectJoinList(Building.class,
                    new MPJLambdaWrapper<DeptBuilding>()
                            .eq(DeptBuilding::getDeptId, dept.getId())
                            .leftJoin(Building.class, on->on.eq(DeptBuilding::getBuildingId, Building::getId))
                            .selectAll(Building.class)
            );
            vo.setDept(dept);
            vo.setBuildings(buildings);
            return vo;
        }).toList();
        List<LaboratoryVo> laboratories = laboratoryUserMapper.selectJoinList(LaboratoryVo.class,
                new MPJLambdaWrapper<LaboratoryUser>()
                        .selectAll(Laboratory.class)
                        .leftJoin(Laboratory.class, on->on.eq(LaboratoryUser::getUserId, user.getId())
                                .eq(LaboratoryUser::getLaboratoryId, Laboratory::getId))
        );
        laboratories = laboratories.stream().filter(Objects::nonNull).toList();
        laboratories.forEach(one->{
            var Q = new MPJLambdaWrapper<LaboratoryManager>()
                    .selectAll(User.class)
                    .eq(LaboratoryManager::getLaboratoryId,one.getId())
                    .leftJoin(User.class,User::getId, LaboratoryManager::getUserId);
            List<UserVo> managers = laboratoryManagerMapper.selectJoinList(UserVo.class,Q);
            one.setManagers(managers);
        });
        List<UserPermission> userPermissions = userPermissionMapper.selectList(
                new LambdaQueryWrapper<UserPermission>().eq(UserPermission::getUserId, user.getId())
        );
        List<UserPermissionVo> vos = userPermissions.stream().map(userPermission -> {
            UserPermissionVo vo = new UserPermissionVo();
            vo.setPermission(userPermission.getPermission());
            vo.setPath(Permissions.pathOf(userPermission.getPermission()));
            return vo;
        }).toList();

        List<Building> buildings = laboratories.stream().map(LaboratoryVo::getBelongToBuilding).collect(Collectors.toSet())
                        .stream().map(buildingId -> buildingMapper.selectById(buildingId)
                ).toList();

        userBizVo.setDepts(deptVos);
        userBizVo.setLaboratories(laboratories);
        userBizVo.setPermissions(vos);
        userBizVo.setBuildings(buildings);
        return userBizVo;
    }

    @Override
    public List<User> visible() {
        Long doUserId = StpUtil.getLoginIdAsLong();
        User self = baseMapper.selectById(doUserId);
        if (self == null) {
            return new ArrayList<>();
        }
        
        // 使用path路径枚举查询所有子孙用户（包含自身）
        String selfPath = self.getPath();
        if (selfPath == null || selfPath.isEmpty()) {
            // 如果当前用户没有path，说明是根用户，只返回自身
            return List.of(self);
        }
        
        // 查询所有path以当前用户path开头的用户（包含自身和所有子孙）
        // 例如：当前用户path为 "/1/"，查询 path LIKE "/1/%" OR path = "/1/" 的所有用户
        List<User> descendants = this.lambdaQuery()
                .and(wrapper -> wrapper
                        .likeRight(User::getPath, selfPath)  // 匹配所有子孙：path LIKE "/1/%"
                        .or()
                        .eq(User::getPath, selfPath)         // 匹配自身：path = "/1/"
                )
                .list();
        
        return descendants;
    }
}

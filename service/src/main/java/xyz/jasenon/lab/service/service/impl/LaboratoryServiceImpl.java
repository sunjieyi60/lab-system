package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.http.HttpStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.laboratory.CreateLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.DeleteLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.EditLaboratory;
import xyz.jasenon.lab.service.mapper.*;
import xyz.jasenon.lab.service.service.ILaboratoryService;
import xyz.jasenon.lab.service.service.IUserService;
import xyz.jasenon.lab.service.vo.base.LaboratoryVo;
import xyz.jasenon.lab.service.vo.base.UserVo;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Slf4j
@Service
public class LaboratoryServiceImpl extends ServiceImpl<LaboratoryMapper, Laboratory> implements ILaboratoryService {

    @Autowired
    private LaboratoryManagerMapper laboratoryManagerMapper;
    @Autowired
    private LaboratoryUserMapper laboratoryUserMapper;
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private DeptUserMapper deptUserMapper;
    @Autowired
    private IUserService userService;

    /**
     *
     * @param createLaboratory
     *  通过{@link xyz.jasenon.lab.service.service.IUserService }获取visibletree 得到可以指派为实验室管理的用户
     * @return
     */
    @Override
    public R createLaboratory(CreateLaboratory createLaboratory) {

        Long doUserId = StpUtil.getLoginIdAsLong();
        Laboratory laboratory = new Laboratory()
                .setLaboratoryId(createLaboratory.getLaboratoryId())
                .setLaboratoryName(createLaboratory.getLaboratoryName())
                .setBelongToBuilding(createLaboratory.getBelongToBuilding())
                .setArea(createLaboratory.getArea())
                .setClassCapacity(createLaboratory.getClassCapacity())
                .setSecurityLevel(createLaboratory.getSecurityLevel())
                .setIntro(createLaboratory.getIntro());

        for(Long deptId : createLaboratory.getBelongToDeptIds()){
            Dept dept = deptMapper.selectById(deptId);
            if(dept == null){
                return R.fail("部门不存在");
            }
        }
        laboratory.setBelongToDepts(createLaboratory.getBelongToDeptIds());
        this.save(laboratory);

        Long newLabId = laboratory.getId();
        List<User> visibleUsers = userService.visible();
        Set<Long> visibleUserIdSet = new HashSet<>(visibleUsers.stream().map(User::getId).toList());

        // 负责人：若入参带了 userIds 且不为空则关联（去重），否则用当前登录用户
        if (createLaboratory.getUserIds() != null && !createLaboratory.getUserIds().isEmpty()) {
            Set<Long> managerUserIds = new LinkedHashSet<>();
            for (Long userId : createLaboratory.getUserIds()) {
                if (visibleUserIdSet.contains(userId)) {
                    managerUserIds.add(userId);
                }
            }
            for (Long userId : managerUserIds) {
                laboratoryManagerMapper.insert(new LaboratoryManager()
                        .setLaboratoryId(newLabId)
                        .setUserId(userId));
            }
        } else {
            laboratoryManagerMapper.insert(new LaboratoryManager()
                    .setLaboratoryId(newLabId)
                    .setUserId(doUserId));
        }

        // 实验室可见用户：创建人 + 可见树用户 + 入参中已校验的负责人，(laboratoryId, userId) 仅插入一次
        Set<Long> laboratoryVisibleUserIds = new LinkedHashSet<>();
        laboratoryVisibleUserIds.add(doUserId);
        for (User u : visibleUsers) {
            laboratoryVisibleUserIds.add(u.getId());
        }
        if (createLaboratory.getUserIds() != null && !createLaboratory.getUserIds().isEmpty()) {
            for (Long userId : createLaboratory.getUserIds()) {
                if (visibleUserIdSet.contains(userId)) {
                    laboratoryVisibleUserIds.add(userId);
                }
            }
        }
        for (Long userId : laboratoryVisibleUserIds) {
            laboratoryUserMapper.insert(new LaboratoryUser()
                    .setLaboratoryId(newLabId)
                    .setUserId(userId));
        }
        return R.success("创建成功");
    }

    @Override
    public R editLaboratory(EditLaboratory editLaboratory) {
        Long doUserId = StpUtil.getLoginIdAsLong();
        Laboratory laboratory = this.getById(editLaboratory.getId());
        if (laboratory == null) {
            return R.fail("实验室不存在");
        }
        List<Long> allowedDepts = deptUserMapper.selectList(new LambdaQueryWrapper<DeptUser>()
                .eq(DeptUser::getUserId, doUserId)).stream().map(DeptUser::getDeptId).toList();

        boolean allMatch = !new HashSet<>(allowedDepts).containsAll(editLaboratory.getBelongToDeptIds());
        if (allMatch){
            return R.fail("你无权调整实验室的所属部门，因为你不属于该部门");
        }

        CopyOptions copyOptions = CopyOptions.create()
                .setIgnoreProperties("id","createTime")
                .ignoreNullValue();

        Laboratory edit = new Laboratory()
                .setLaboratoryId(editLaboratory.getLaboratoryId())
                .setLaboratoryName(editLaboratory.getLaboratoryName())
                .setBelongToBuilding(editLaboratory.getBelongToBuilding())
                .setBelongToDepts(editLaboratory.getBelongToDeptIds())
                .setArea(editLaboratory.getArea())
                .setClassCapacity(editLaboratory.getClassCapacity())
                .setSecurityLevel(editLaboratory.getSecurityLevel());

        BeanUtil.copyProperties(edit, laboratory, copyOptions);
        this.updateById(laboratory);

        return R.success("编辑成功");
    }

    @Override
    public R deleteLaboratory(DeleteLaboratory deleteLaboratory) {
        this.removeById(deleteLaboratory.getId());
        return R.success("删除成功");
    }

    @Override
    public R editManagers(Long laboratoryId, List<Long> userIds) {

        Long doUserId = StpUtil.getLoginIdAsLong();
        LaboratoryUser laboratoryUser = laboratoryUserMapper.selectOne(new LambdaQueryWrapper<LaboratoryUser>()
                .eq(LaboratoryUser::getUserId, doUserId)
                .eq(LaboratoryUser::getLaboratoryId, laboratoryId));
        if (laboratoryUser == null){
            return R.fail("你没有权限编辑该实验室的管理员");
        }

        if (userIds !=null && !userIds.isEmpty()){
            List<LaboratoryManager> managers = laboratoryManagerMapper.selectList(
                    new LambdaQueryWrapper<LaboratoryManager>()
                            .eq(LaboratoryManager::getLaboratoryId, laboratoryId)
            );
            boolean different = new HashSet<>(managers.stream().map(LaboratoryManager::getUserId).toList())
                    .containsAll(userIds);
            if (different){
                var visible = userService.visible().stream().toList();
                boolean allUserVisible = new HashSet<>(visible.stream().map(User::getId).toList()).containsAll(userIds);
                if (!allUserVisible){
                    List<Long> unvisibleUserIds = userIds.stream().filter(u -> !visible.stream().map(User::getId).toList().contains(u)).toList();
                    return R.fail(HttpStatus.HTTP_BAD_REQUEST,"你无权调整实验室的负责人，因为该用户不由你管辖", unvisibleUserIds);
                }
                laboratoryManagerMapper.deleteByIds(managers);
                List<LaboratoryManager> newManagers = userIds.stream().map(userId -> new LaboratoryManager()
                        .setLaboratoryId(laboratoryId)
                        .setUserId(userId)).toList();
                laboratoryManagerMapper.insert(newManagers);
            }
        }
        return R.fail("负责人不能为空");
    }

    @Override
    public R getLaboratoryDetailById(Long laboratoryId) {
        Long doUserId = StpUtil.getLoginIdAsLong();
        LaboratoryUser laboratoryUser = laboratoryUserMapper.selectOne(new LambdaQueryWrapper<LaboratoryUser>()
                .eq(LaboratoryUser::getUserId, doUserId)
                .eq(LaboratoryUser::getLaboratoryId, laboratoryId));
        if (laboratoryUser == null){
            return R.fail("你没有权限查看该实验室的详情");
        }


        List<UserVo> managersVo = laboratoryManagerMapper.selectJoinList(UserVo.class,
                new MPJLambdaWrapper<LaboratoryManager>()
                        .eq(LaboratoryManager::getLaboratoryId, laboratoryId)
                        .leftJoin(User.class, User::getId, LaboratoryManager::getUserId)
                        .selectAll(User.class)
        );

        LaboratoryVo vo = this.baseMapper.selectJoinOne(LaboratoryVo.class,
                new MPJLambdaWrapper<Laboratory>()
                        .eq(Laboratory::getId, laboratoryId)
                        .selectAll(Laboratory.class));
        vo.setManagers(managersVo);
        return R.success(vo,"查询成功");
    }

}

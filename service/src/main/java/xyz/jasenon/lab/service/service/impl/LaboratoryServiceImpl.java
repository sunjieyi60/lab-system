package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.stream.CollectorUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.jasenon.lab.common.entity.base.*;
import xyz.jasenon.lab.common.utils.FilterKit;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.laboratory.CreateLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.DeleteLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.EditLaboratory;
import xyz.jasenon.lab.service.mapper.*;
import xyz.jasenon.lab.service.service.ILaboratoryService;
import xyz.jasenon.lab.service.service.IUserService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Service
public class LaboratoryServiceImpl extends ServiceImpl<LaboratoryMapper, Laboratory> implements ILaboratoryService {

    @Autowired
    private LaboratoryManagerMapper laboratoryManagerMapper;
    @Autowired
    private LaboratoryUserMapper laboratoryUserMapper;
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private UserMapper userMapper;
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

        // 负责人：若入参带了 userIds且不为空则关联，否则用当前登录用户
        if (createLaboratory.getUserIds() != null && !createLaboratory.getUserIds().isEmpty()) {
            List<Long> visibleUserIds = userService.visible().stream().map(User::getId).toList();
            for (Long userId : createLaboratory.getUserIds()) {
                if (visibleUserIds.contains(userId)) {
                    //补充可见性
                    LaboratoryUser laboratoryUser = new LaboratoryUser()
                            .setLaboratoryId(laboratory.getId())
                            .setUserId(userId);
                    laboratoryUserMapper.insert(laboratoryUser);
                    //添加管理信息
                    LaboratoryManager laboratoryManager = new LaboratoryManager()
                            .setLaboratoryId(laboratory.getId())
                            .setUserId(userId);
                    laboratoryManagerMapper.insert(laboratoryManager);
                }
            }
        }else{
            LaboratoryManager laboratoryManager = new LaboratoryManager()
                    .setLaboratoryId(laboratory.getId())
                    .setUserId(doUserId);
            laboratoryManagerMapper.insert(laboratoryManager);
        }

        // 创建该实验室的用户对实验室默认可见
        LaboratoryUser laboratoryUser = new LaboratoryUser()
                .setLaboratoryId(laboratory.getId())
                .setUserId(doUserId);
        laboratoryUserMapper.insert(laboratoryUser);

        User user = userMapper.selectById(doUserId);
        List<User> fathers = fathers(user, new ArrayList<>());
        for(User father : fathers){
            LaboratoryUser fzUser = new LaboratoryUser()
                    .setLaboratoryId(laboratory.getId())
                    .setUserId(father.getId());
            laboratoryUserMapper.insert(fzUser);
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
            // 清除旧的记录
            laboratoryManagerMapper.delete(new LambdaUpdateWrapper<LaboratoryManager>()
                    .eq(LaboratoryManager::getLaboratoryId, laboratoryId));

            // 防止unique索引出错导致回滚  手动去重
            List<LaboratoryManager> updates = userIds.stream().map(userId -> new LaboratoryManager()
                    .setLaboratoryId(laboratoryId)
                    .setUserId(userId)).filter(FilterKit.distinctByKey(LaboratoryManager::getUserId)).toList();

            laboratoryManagerMapper.insert(updates);
            return R.success("编辑成功");
        }
        return R.fail("负责人不能为空");
    }

    private List<User> fathers(User user, List<User> result){
        List<User> fathers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getId, user.getCreateBy()));
        if(fathers.isEmpty()){
            return result;
        }
        result.addAll(fathers);
        for(User father : fathers){
            fathers(father, result);
        }
        return result;
    }

}

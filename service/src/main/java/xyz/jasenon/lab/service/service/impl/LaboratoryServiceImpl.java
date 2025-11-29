package xyz.jasenon.lab.service.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.*;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.laboratory.CreateLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.DeleteLaboratory;
import xyz.jasenon.lab.service.dto.laboratory.EditLaboratory;
import xyz.jasenon.lab.service.mapper.*;
import xyz.jasenon.lab.service.service.ILaboratoryService;

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

    @Override
    public R createLaboratory(CreateLaboratory createLaboratory) {

        Long doUserId = StpUtil.getLoginIdAsLong();
        Laboratory laboratory = new Laboratory()
                .setLaboratoryId(createLaboratory.getLaboratoryId())
                .setLaboratoryName(createLaboratory.getLaboratoryName())
                .setBelongToBuilding(createLaboratory.getBelongToBuilding())
                .setArea(createLaboratory.getArea())
                .setClassCapacity(createLaboratory.getClassCapacity())
                .setSecurityLevel(createLaboratory.getSecurityLevel());

        for(Long deptId : createLaboratory.getBelongToDeptIds()){
            Dept dept = deptMapper.selectById(deptId);
            if(dept == null){
                return R.fail("部门不存在");
            }
        }
        laboratory.setBelongToDepts(createLaboratory.getBelongToDeptIds());
        this.save(laboratory);

        LaboratoryManager laboratoryManager = new LaboratoryManager()
                .setLaboratoryId(laboratory.getId())
                .setUserId(doUserId);
        laboratoryManagerMapper.insert(laboratoryManager);

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

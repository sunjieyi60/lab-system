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
import xyz.jasenon.lab.service.dto.CreateLaboratory;
import xyz.jasenon.lab.service.dto.DeleteLaboratory;
import xyz.jasenon.lab.service.dto.EditLaboratory;
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
                .laboratoryId(createLaboratory.laboratoryId())
                .laboratoryName(createLaboratory.laboratoryName())
                .belongToBuilding(createLaboratory.belongToBuilding())
                .area(createLaboratory.area())
                .classCapacity(createLaboratory.classCapacity())
                .securityLevel(createLaboratory.securityLevel());

        for(Long deptId : createLaboratory.belongToDeptIds()){
            Dept dept = deptMapper.selectById(deptId);
            if(dept == null){
                return R.fail("部门不存在");
            }
        }
        laboratory.belongToDepts(createLaboratory.belongToDeptIds());
        this.save(laboratory);

        LaboratoryManager laboratoryManager = new LaboratoryManager()
                .laboratoryId(laboratory.getId())
                .userId(doUserId);
        laboratoryManagerMapper.insert(laboratoryManager);

        LaboratoryUser laboratoryUser = new LaboratoryUser()
                .laboratoryId(laboratory.getId())
                .userId(doUserId);
        laboratoryUserMapper.insert(laboratoryUser);

        User user = userMapper.selectById(doUserId);
        List<User> fathers = fathers(user, new ArrayList<>());
        for(User father : fathers){
            LaboratoryUser fzUser = new LaboratoryUser()
                    .laboratoryId(laboratory.getId())
                    .userId(father.getId());
            laboratoryUserMapper.insert(fzUser);
        }
        return R.success("创建成功");
    }

    @Override
    public R editLaboratory(EditLaboratory editLaboratory) {
        Long doUserId = StpUtil.getLoginIdAsLong();
        Laboratory laboratory = this.getById(editLaboratory.id());
        if (laboratory == null) {
            return R.fail("实验室不存在");
        }
        List<Long> allowedDepts = deptUserMapper.selectList(new LambdaQueryWrapper<DeptUser>()
                .eq(DeptUser::getUserId, doUserId)).stream().map(DeptUser::getDeptId).toList();

        boolean allMatch = !new HashSet<>(allowedDepts).containsAll(editLaboratory.belongToDeptIds());
        if (allMatch){
            return R.fail("你无权调整实验室的所属部门，因为你不属于该部门");
        }

        CopyOptions copyOptions = CopyOptions.create()
                .setIgnoreProperties("id","createTime")
                .ignoreNullValue();

        Laboratory edit = new Laboratory()
                .laboratoryId(editLaboratory.laboratoryId())
                .laboratoryName(editLaboratory.laboratoryName())
                .belongToBuilding(editLaboratory.belongToBuilding())
                .belongToDepts(editLaboratory.belongToDeptIds())
                .area(editLaboratory.area())
                .classCapacity(editLaboratory.classCapacity())
                .securityLevel(editLaboratory.securityLevel());

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

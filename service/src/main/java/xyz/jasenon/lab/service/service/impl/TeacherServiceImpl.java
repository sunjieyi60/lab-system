package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.class_time_table.Teacher;
import xyz.jasenon.lab.service.mapper.TeacherMapper;
import xyz.jasenon.lab.service.service.ITeacherService;

/**
 * @author Jasenon_ce
 * @date 2025/11/27
 */
@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements ITeacherService {
}

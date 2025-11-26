package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.LaboratoryUser;
import xyz.jasenon.lab.service.mapper.LaboratoryUserMapper;
import xyz.jasenon.lab.service.service.ILaboratoryUserService;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Service
public class LaboratoryUserServiceImpl extends ServiceImpl<LaboratoryUserMapper, LaboratoryUser> implements ILaboratoryUserService {
}

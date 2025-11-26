package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.service.entity.UserPermission;
import xyz.jasenon.lab.service.mapper.UserPermissionMapper;
import xyz.jasenon.lab.service.service.IUserPermissionService;

/**
 * @author Jasenon_ce
 * @date 2025/11/26
 */
@Service
public class UserPermissionServiceImpl extends ServiceImpl<UserPermissionMapper, UserPermission> implements IUserPermissionService {
}

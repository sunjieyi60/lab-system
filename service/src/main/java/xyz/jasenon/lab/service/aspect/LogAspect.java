package xyz.jasenon.lab.service.aspect;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.common.entity.log.OperationLog;
import xyz.jasenon.lab.common.utils.ExpressionEvaluator;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.annotation.log.LogPoint;
import xyz.jasenon.lab.service.constants.Permissions;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;
import xyz.jasenon.lab.service.dto.log.OperationLogParts;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.dto.user.DeleteUser;
import xyz.jasenon.lab.service.dto.user.EditUser;
import xyz.jasenon.lab.service.log.LogTaskManager;
import xyz.jasenon.lab.service.log.ScheduleConfigLogInterpreter;
import xyz.jasenon.lab.service.log.TaskLogInterpreter;
import xyz.jasenon.lab.service.log.UserLogInterpreter;
import xyz.jasenon.lab.service.quartz.model.ScheduleConfigRoot;
import xyz.jasenon.lab.service.service.IUserService;
import xyz.jasenon.lab.service.vo.base.UserBizVo;
import xyz.jasenon.lab.service.vo.base.UserPermissionVo;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
public class LogAspect {

    // 支持不同返回类型的 SpEL 解析
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final Map<Class<?>, LogInterpreter<?>> interpreters = new ConcurrentHashMap<>();
    private final LogTaskManager logTaskManager;
    private final TaskLogInterpreter taskLogInterpreter;
    private final ScheduleConfigLogInterpreter scheduleConfigLogInterpreter;
    private final UserLogInterpreter userLogInterpreter;
    private final IUserService userService;

    public LogAspect(LogTaskManager logTaskManager,
                     TaskLogInterpreter taskLogInterpreter,
                     ScheduleConfigLogInterpreter scheduleConfigLogInterpreter,
                     UserLogInterpreter userLogInterpreter,
                     IUserService userService) {
        this.logTaskManager = logTaskManager;
        this.taskLogInterpreter = taskLogInterpreter;
        this.scheduleConfigLogInterpreter = scheduleConfigLogInterpreter;
        this.userLogInterpreter = userLogInterpreter;
        this.userService = userService;
    }

    @Pointcut("@annotation(xyz.jasenon.lab.service.annotation.log.LogPoint)")
    public void logPointcut() {
    }

    @PostConstruct
    public void init() {
        // 按需注册解释器，可后续扩展为 Spring 容器扫描或配置化
        interpreters.put(CreateBuilding.class, payload -> {
            CreateBuilding dto = (CreateBuilding) payload;
            return "创建了楼栋[" + dto.getBuildingName() + "]，部门ID列表" + dto.getDeptIds();
        });
        interpreters.put(EditBuilding.class, payload -> {
            EditBuilding dto = (EditBuilding) payload;
            return "编辑了楼栋[" + dto.getBuildingId() + ":" + dto.getBuildingName() + "]";
        });
        interpreters.put(Task.class, taskLogInterpreter);
        interpreters.put(ScheduleConfigRoot.class, scheduleConfigLogInterpreter);
        interpreters.put(CreateUser.class, (LogInterpreter<CreateUser>) userLogInterpreter::renderCreate);
        interpreters.put(EditUser.class, (LogInterpreter<EditUser>) userLogInterpreter::renderEdit);
        interpreters.put(DeleteUser.class, (LogInterpreter<DeleteUser>) userLogInterpreter::renderDelete);
    }

    @After("logPointcut()")
    public void log(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogPoint logPoint = method.getAnnotation(LogPoint.class);
        if (logPoint == null) {
            return;
        }

        Class<?> targetClass = joinPoint.getTarget().getClass();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = evaluator.createEvaluationContext(joinPoint.getTarget(), targetClass, method, args);
        AnnotatedElementKey key = new AnnotatedElementKey(method, targetClass);

        Object payload = evaluatePayload(logPoint, key, context);
        Object interpreted = interpret(logPoint.clazz(), payload);

        String title = safeEval(logPoint.title(), key, context);

        // 组装操作日志实体，交给异步任务管理器落库
        OperationLog entity = new OperationLog()
                .setOperatorId(currentUserId())
                .setOperatorAccount(currentUserAccount())
                .setLogType(title)
                .setOperateTime(LocalDateTime.now());

        // 补全操作者姓名、角色
        fillOperatorNameAndRole(entity);

        if (interpreted instanceof OperationLogParts) {
            OperationLogParts parts = (OperationLogParts) interpreted;
            entity.setRoom(parts.getRoom());
            entity.setDevice(parts.getDevice());
            entity.setOperateWay(parts.getOperateWay());
            entity.setContent(parts.getContent());
        } else {
            // 非设备控制、报警联动等复杂场景，统一视为手动操作
            entity.setOperateWay("手动");
            String content = interpreted != null ? interpreted.toString() : safeEval(logPoint.content(), key, context);
            entity.setContent(content);
        }

        logTaskManager.submitOperationLog(entity);
    }

    private Object evaluatePayload(LogPoint logPoint, AnnotatedElementKey key, EvaluationContext context) {
        if (!StringUtils.hasText(logPoint.sqEl())) {
            return null;
        }
        try {
            return evaluator.condition(logPoint.sqEl(), key, context, Object.class);
        } catch (Exception e) {
            log.warn("LogPoint sqEl 解析失败: {}", logPoint.sqEl(), e);
            return null;
        }
    }

    private <T> Object interpret(Class<T> clazz, Object payload) {
        if (payload == null || clazz == Void.class) {
            return null;
        }
        LogInterpreter<?> raw = interpreters.get(clazz);
        if (raw == null) {
            return null;
        }
        if (!clazz.isInstance(payload)) {
            log.warn("LogPoint payload 类型不匹配, 期望: {}, 实际: {}", clazz.getName(), payload.getClass().getName());
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            LogInterpreter<T> interpreter = (LogInterpreter<T>) raw;
            return interpreter.render(clazz.cast(payload));
        } catch (Exception e) {
            log.warn("LogPoint 解释器执行失败, clazz={}", clazz.getSimpleName(), e);
            return null;
        }
    }

    private String safeEval(String expr, AnnotatedElementKey key, EvaluationContext context) {
        if (!StringUtils.hasText(expr)) {
            return expr;
        }
        try {
            return evaluator.condition(expr, key, context, String.class);
        } catch (Exception e) {
            log.warn("LogPoint 表达式解析失败: {}", expr, e);
            return expr;
        }
    }

    private Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (NotLoginException e) {
            return null;
        }
    }

    private String currentUserAccount() {
        try {
            Object id = StpUtil.getLoginIdDefaultNull();
            return id != null ? String.valueOf(id) : null;
        } catch (NotLoginException e) {
            return null;
        }
    }

    /**
     * 从当前用户详情补全操作人姓名、角色，便于前端按姓名、角色筛选。
     */
    private void fillOperatorNameAndRole(OperationLog entity) {
        try {
            UserBizVo vo = userService.getCurrentUserDetail();
            if (vo == null ) {
                return;
            }
            if (vo.getRealName() != null) {
                entity.setOperatorName(vo.getRealName());
            }
            List<UserPermissionVo> perms = vo.getPermissions();
            if (perms != null && !perms.isEmpty()) {
                entity.setOperatorRole(deriveOperatorRole(perms));
            }
        } catch (Exception e) {
            log.debug("补全操作人姓名/角色失败，跳过: {}", e.getMessage());
        }
    }

    /**
     * 根据权限列表推导展示用角色：有 ROOT 为超级管理员，有其它管理类权限为管理员，否则普通用户。
     */
    private String deriveOperatorRole(List<UserPermissionVo> perms) {
        if (perms == null || perms.isEmpty()) {
            return "普通用户";
        }
        for (UserPermissionVo p : perms) {
            if (p == null || p.getPermission() == null) continue;
            Permissions perm = p.getPermission();
            if (perm == Permissions.ROOT) return "超级管理员";
            if (perm == Permissions.USER || perm == Permissions.BASE_SETTINGS || perm == Permissions.CONTROL_CENTER
                    || perm == Permissions.DATA_ANALYSIS || perm == Permissions.ACADEMIC_AFFAIRS_MANAGEMENT) {
                return "管理员";
            }
        }
        return "普通用户";
    }

}

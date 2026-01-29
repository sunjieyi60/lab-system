package xyz.jasenon.lab.service.aspect;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
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
import xyz.jasenon.lab.common.utils.ExpressionEvaluator;
import xyz.jasenon.lab.common.entity.log.OperationLog;
import xyz.jasenon.lab.service.annotation.log.LogPoint;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;
import xyz.jasenon.lab.common.dto.task.Task;
import xyz.jasenon.lab.service.dto.log.OperationLogParts;
import xyz.jasenon.lab.service.dto.user.CreateUser;
import xyz.jasenon.lab.service.dto.user.DeleteUser;
import xyz.jasenon.lab.service.dto.user.EditUser;
import xyz.jasenon.lab.service.log.LogTaskManager;
import xyz.jasenon.lab.service.log.TaskLogInterpreter;
import xyz.jasenon.lab.service.log.ScheduleConfigLogInterpreter;
import xyz.jasenon.lab.service.log.UserLogInterpreter;
import xyz.jasenon.lab.service.quartz.model.ScheduleConfigRoot;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

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

    public LogAspect(LogTaskManager logTaskManager,
                     TaskLogInterpreter taskLogInterpreter,
                     ScheduleConfigLogInterpreter scheduleConfigLogInterpreter,
                     UserLogInterpreter userLogInterpreter) {
        this.logTaskManager = logTaskManager;
        this.taskLogInterpreter = taskLogInterpreter;
        this.scheduleConfigLogInterpreter = scheduleConfigLogInterpreter;
        this.userLogInterpreter = userLogInterpreter;
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

        if (interpreted instanceof OperationLogParts) {
            OperationLogParts parts = (OperationLogParts) interpreted;
            entity.setRoom(parts.getRoom());
            entity.setDevice(parts.getDevice());
            entity.setOperateWay(parts.getOperateWay());
            entity.setContent(parts.getContent());
        } else {
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

}

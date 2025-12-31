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
import xyz.jasenon.lab.service.annotation.log.LogPoint;
import xyz.jasenon.lab.service.dto.building.CreateBuilding;
import xyz.jasenon.lab.service.dto.building.EditBuilding;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
public class LogAspect {

    // 支持不同返回类型的 SpEL 解析
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final Map<Class<?>, LogInterpreter<?>> interpreters = new ConcurrentHashMap<>();

    @Pointcut("@annotation(xyz.jasenon.lab.service.annotation.log.LogPoint)")
    public void logPointcut() {
    }

    @PostConstruct
    public void init() {
        // 按需注册解释器，可后续扩展为 Spring 容器扫描或配置化
        interpreters.put(CreateBuilding.class, payload -> {
            CreateBuilding dto = (CreateBuilding) payload;
            return "用户" + currentUser() + "创建了楼栋[" + dto.getBuildingName() + "]，部门ID列表" + dto.getDeptIds();
        });
        interpreters.put(EditBuilding.class, payload -> {
            EditBuilding dto = (EditBuilding) payload;
            return "用户" + currentUser() + "编辑了楼栋[" + dto.getBuildingId() + ":" + dto.getBuildingName() + "]";
        });
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
        String interpreted = interpret(logPoint.clazz(), payload);

        String title = safeEval(logPoint.title(), key, context);
        String content = interpreted != null ? interpreted : safeEval(logPoint.content(), key, context);

        log.info("[{}] {}", title, content);
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

    private <T> String interpret(Class<T> clazz, Object payload) {
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

}

package xyz.jasenon.lab.common.utils;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jasenon_ce
 * @date 2025/10/25
 */
public class ExpressionRootObject {
    private final Object object;
    private final Object[] args;
    private final Method method;
    private final ParameterNameDiscoverer parameterNameDiscoverer;
    private Map<String, Object> parameterMap;

    public ExpressionRootObject(Object object, Object[] args, Method method) {
        this.object = object;
        this.args = args;
        this.method = method;
        this.parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        this.parameterMap = createParameterMap();
    }

    public Object getObject() {
        return object;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object get(String parameterName) {
        return parameterMap.get(parameterName);
    }

    private Map<String, Object> createParameterMap() {
        Map<String, Object> map = new HashMap<>();
        if (method != null && args != null) {
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    if (i < args.length) {
                        map.put(parameterNames[i], args[i]);
                    }
                }
            }
        }
        return map;
    }
}

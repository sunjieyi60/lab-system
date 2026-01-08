package xyz.jasenon.lab.service.quartz.check;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.text.MessageFormat;
import java.util.Map;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
public class ConditionExprChecker {

    private static final ExpressionParser parser = new SpelExpressionParser();

    public static Result<Boolean> eval(String expr, Map<String, Map<String, Object>> vars) {
        Boolean result = parser.parseExpression(expr).getValue(vars, Boolean.class);
        if (result == null) {
            throw new IllegalArgumentException(MessageFormat.format("表达式执行结果为空，请检查表达式是否正确: {0},Context上下文：{1}", expr, vars));
        }
        if (result) {
            return Result.success(true);
        }else{
            return Result.error(false, "表达式条件不满足");
        }
    }

}

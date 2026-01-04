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

    private final ExpressionParser parser = new SpelExpressionParser();

    public Result<Boolean> eval(String expr, Map<String, Object> vars) {
        Boolean result = parser.parseExpression(expr).getValue(vars, Boolean.class);
        if (result == null) {
            return Result.error(false, MessageFormat.format("表达式执行结果为空，请检查表达式是否正确: {0},Context上下文：{1}", expr, vars));
        }
        return Result.success(result);
    }

}

package xyz.jasenon.lab.service.quartz.check;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.context.expression.MapAccessor;

import java.text.MessageFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
public class ConditionExprChecker {

    private static final ExpressionParser parser = new SpelExpressionParser();
    
    /**
     * 匹配 #{数字} 格式的占位符
     * 例如: #{1789569715678901234} 会被转换为 ['1789569715678901234']
     */
    private static final Pattern DATA_ID_PATTERN = Pattern.compile("#\\{(\\d+)\\}");

    /**
     * 预处理表达式，将 #{数字} 替换为正确的SpEL Map访问语法
     */
    private static String preprocessExpression(String expr) {
        if (expr == null || expr.isEmpty()) {
            return expr;
        }
        
        Matcher matcher = DATA_ID_PATTERN.matcher(expr);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String replacement = "#root['" + matcher.group(1) + "']";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    public static Result<Boolean> eval(String expr, Map<String, Map<String, Object>> vars) {
        String processedExpr = preprocessExpression(expr);
        
        // 创建 StandardEvaluationContext，支持 Map 的属性访问
        StandardEvaluationContext context = new StandardEvaluationContext(vars);
        // 添加 MapAccessor，允许通过 .property 语法访问 Map 中的键
        context.addPropertyAccessor(new MapAccessor());
        
        Boolean result = parser.parseExpression(processedExpr).getValue(context, Boolean.class);
        if (result == null) {
            throw new IllegalArgumentException(MessageFormat.format("表达式执行结果为空，请检查表达式是否正确: {0},Context上下文：{1}", processedExpr, vars));
        }
        if (result) {
            return Result.success(true);
        }else{
            return Result.error(false, "表达式条件不满足");
        }
    }

}

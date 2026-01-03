package xyz.jasenon.lab.schedule.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.schedule.model.ConditionGroup;
import xyz.jasenon.lab.schedule.model.ConditionItem;
import xyz.jasenon.lab.schedule.model.DataGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConditionService {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Data
    public static class ConditionEval {
        private Long id;
        private String expr;
        private Boolean result;
        private String desc;
    }

    @Data
    public static class ConditionSummary {
        private boolean passed;
        private List<ConditionEval> details;
    }

    public ConditionSummary evaluate(ConditionGroup group, List<DataGroup> dataGroups) {
        Map<Long, Object> dataMap = new HashMap<>();
        for (DataGroup d : dataGroups) {
            dataMap.put(d.getId(), d.getMockValue());
        }
        EvaluationContext ctx = new StandardEvaluationContext();
        ctx.setVariable("data", dataMap);

        List<ConditionEval> list = group.getConditions().stream().map(c -> eval(c, ctx)).toList();
        boolean passed;
        if ("ALL".equalsIgnoreCase(group.getLogic())) {
            passed = list.stream().allMatch(e -> Boolean.TRUE.equals(e.getResult()));
        } else {
            passed = list.stream().anyMatch(e -> Boolean.TRUE.equals(e.getResult()));
        }
        ConditionSummary summary = new ConditionSummary();
        summary.setPassed(passed);
        summary.setDetails(list);
        return summary;
    }

    private ConditionEval eval(ConditionItem c, EvaluationContext ctx) {
        ConditionEval eval = new ConditionEval();
        eval.setId(c.getId());
        eval.setExpr(c.getExpr());
        eval.setDesc(c.getDesc());
        try {
            Boolean ok = parser.parseExpression(c.getExpr()).getValue(ctx, Boolean.class);
            eval.setResult(ok);
        } catch (Exception e) {
            eval.setResult(false);
        }
        return eval;
    }
}


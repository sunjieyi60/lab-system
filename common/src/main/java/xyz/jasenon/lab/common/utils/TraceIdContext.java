package xyz.jasenon.lab.common.utils;

import cn.hutool.core.lang.Snowflake;
import org.slf4j.MDC;
import xyz.jasenon.lab.common.Const;

/**
 * TraceId 上下文工具
 * 用于生成、存储和传递链路追踪 ID
 * 
 * @author Jasenon_ce
 * @date 2026/3/17
 */
public class TraceIdContext {

    private static final Snowflake snowflake = new Snowflake();
    /**
     * 生成新的 TraceId (简化版 UUID)
     */
    public static String generate() {
        return Const.TRACE_ID_KEY + Const.Key.SUFFIX + snowflake.nextIdStr();
    }

    /**
     * 放入 MDC（日志上下文）
     */
    public static void put(String traceId) {
        MDC.put(Const.TRACE_ID_KEY, traceId);
    }

    /**
     * 获取当前 TraceId
     */
    public static String get() {
        return MDC.get(Const.TRACE_ID_KEY);
    }

    /**
     * 获取或生成（不存在则创建）
     */
    public static String getOrGenerate() {
        String traceId = get();
        if (traceId == null) {
            traceId = generate();
            put(traceId);
        }
        return traceId;
    }

    /**
     * 清理（防止线程池复用污染）
     */
    public static void clear() {
        MDC.remove(Const.TRACE_ID_KEY);
    }
}

package xyz.jasenon.lab.cache.spi;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存服务 SPI 接口
 * 提供基于 Supplier 的 Redis 一级缓存统一抽象
 * 支持 Redis 所有数据结构：String、Hash、List、Set、Sorted Set
 *
 * @author Jasenon
 */
public interface Cache {

    /**
     * 清空所有缓存
     */
    void clearAll();

    // ==================== String 操作 ====================

    /**
     * 获取 String 类型缓存，使用 Supplier 加载
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 缓存值
     */
    <T> T get(String key, Supplier<T> supplier, Class<T> clazz);

    /**
     * 获取 String 类型缓存，使用 Supplier 加载，指定过期时间
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param timeout  过期时间
     * @param unit     时间单位
     * @param <T>      返回值类型
     * @return 缓存值
     */
    <T> T get(String key, Supplier<T> supplier, Class<T> clazz, long timeout, TimeUnit unit);

    /**
     * 获取 String 类型缓存（支持复杂泛型），使用 Supplier 加载
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 缓存值
     */
    <T> T get(String key, Supplier<T> supplier, TypeReference<T> typeRef);

    /**
     * 获取 String 类型缓存（支持复杂泛型），使用 Supplier 加载，指定过期时间
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param timeout  过期时间
     * @param unit     时间单位
     * @param <T>      返回值类型
     * @return 缓存值
     */
    <T> T get(String key, Supplier<T> supplier, TypeReference<T> typeRef, long timeout, TimeUnit unit);

    /**
     * 设置 String 类型缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void set(String key, Object value);

    /**
     * 设置 String 类型缓存，指定过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    void set(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 批量获取 String 类型缓存
     *
     * @param keys     缓存键集合
     * @param supplier 数据加载器（传入缺失的key，返回key-value映射）
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return key-value 映射
     */
    <T> Map<String, T> mget(Collection<String> keys, Supplier<Map<String, T>> supplier, Class<T> clazz);

    /**
     * 批量获取 String 类型缓存（支持复杂泛型）
     *
     * @param keys     缓存键集合
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return key-value 映射
     */
    <T> Map<String, T> mget(Collection<String> keys, Supplier<Map<String, T>> supplier, TypeReference<T> typeRef);

    /**
     * 批量设置 String 类型缓存
     *
     * @param map 缓存键值映射
     */
    void mset(Map<String, Object> map);

    /**
     * 删除 String 类型缓存
     *
     * @param key 缓存键
     * @return 是否成功删除
     */
    boolean delete(String key);

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     * @return 删除数量
     */
    long delete(Collection<String> keys);

    /**
     * 判断 key 是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean hasKey(String key);

    /**
     * 设置过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否成功
     */
    boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取过期时间
     *
     * @param key 缓存键
     * @return 过期时间（秒），-1 表示永不过期，-2 表示不存在
     */
    long getExpire(String key);

    // ==================== Hash 操作 ====================

    /**
     * 获取 Hash 字段值，使用 Supplier 加载
     *
     * @param key      缓存键
     * @param field    字段名
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 字段值
     */
    <T> T hGet(String key, String field, Supplier<T> supplier, Class<T> clazz);

    /**
     * 获取 Hash 字段值（支持复杂泛型），使用 Supplier 加载
     *
     * @param key      缓存键
     * @param field    字段名
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 字段值
     */
    <T> T hGet(String key, String field, Supplier<T> supplier, TypeReference<T> typeRef);

    /**
     * 设置 Hash 字段值
     *
     * @param key   缓存键
     * @param field 字段名
     * @param value 字段值
     */
    void hSet(String key, String field, Object value);

    /**
     * 批量设置 Hash 字段
     *
     * @param key 缓存键
     * @param map 字段映射
     */
    void hSetAll(String key, Map<String, Object> map);

    /**
     * 获取 Hash 所有字段
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 字段映射
     */
    <T> Map<String, T> hGetAll(String key, Supplier<Map<String, T>> supplier, Class<T> clazz);

    /**
     * 获取 Hash 所有字段（支持复杂泛型）
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 字段映射
     */
    <T> Map<String, T> hGetAll(String key, Supplier<Map<String, T>> supplier, TypeReference<T> typeRef);

    /**
     * 批量获取 Hash 字段
     *
     * @param key    缓存键
     * @param fields 字段名集合
     * @param clazz  返回值类型类
     * @param <T>    返回值类型
     * @return 字段值列表
     */
    <T> List<T> hMultiGet(String key, Collection<String> fields, Class<T> clazz);

    /**
     * 批量获取 Hash 字段（支持复杂泛型）
     *
     * @param key     缓存键
     * @param fields  字段名集合
     * @param typeRef 返回值类型引用
     * @param <T>     返回值类型
     * @return 字段值列表
     */
    <T> List<T> hMultiGet(String key, Collection<String> fields, TypeReference<T> typeRef);

    /**
     * 删除 Hash 字段
     *
     * @param key    缓存键
     * @param fields 字段名集合
     * @return 删除数量
     */
    long hDelete(String key, Object... fields);

    /**
     * 判断 Hash 字段是否存在
     *
     * @param key   缓存键
     * @param field 字段名
     * @return 是否存在
     */
    boolean hHasKey(String key, String field);

    /**
     * 获取 Hash 所有字段名
     *
     * @param key 缓存键
     * @return 字段名集合
     */
    Set<String> hKeys(String key);

    /**
     * 获取 Hash 所有值
     *
     * @param key   缓存键
     * @param clazz 返回值类型类
     * @param <T>   返回值类型
     * @return 值集合
     */
    <T> List<T> hValues(String key, Class<T> clazz);

    /**
     * 获取 Hash 所有值（支持复杂泛型）
     *
     * @param key     缓存键
     * @param typeRef 返回值类型引用
     * @param <T>     返回值类型
     * @return 值集合
     */
    <T> List<T> hValues(String key, TypeReference<T> typeRef);

    /**
     * 获取 Hash 大小
     *
     * @param key 缓存键
     * @return 字段数量
     */
    long hSize(String key);

    // ==================== List 操作 ====================

    /**
     * 从左侧推入 List
     *
     * @param key   缓存键
     * @param value 值
     * @return 列表长度
     */
    long lLeftPush(String key, Object value);

    /**
     * 从左侧批量推入 List
     *
     * @param key    缓存键
     * @param values 值集合
     * @return 列表长度
     */
    long lLeftPushAll(String key, Collection<?> values);

    /**
     * 从右侧推入 List
     *
     * @param key   缓存键
     * @param value 值
     * @return 列表长度
     */
    long lRightPush(String key, Object value);

    /**
     * 从右侧批量推入 List
     *
     * @param key    缓存键
     * @param values 值集合
     * @return 列表长度
     */
    long lRightPushAll(String key, Collection<?> values);

    /**
     * 从左侧弹出 List
     *
     * @param key   缓存键
     * @param clazz 返回值类型类
     * @param <T>   返回值类型
     * @return 值
     */
    <T> T lLeftPop(String key, Class<T> clazz);

    /**
     * 从左侧弹出 List（支持复杂泛型）
     *
     * @param key     缓存键
     * @param typeRef 返回值类型引用
     * @param <T>     返回值类型
     * @return 值
     */
    <T> T lLeftPop(String key, TypeReference<T> typeRef);

    /**
     * 从右侧弹出 List
     *
     * @param key   缓存键
     * @param clazz 返回值类型类
     * @param <T>   返回值类型
     * @return 值
     */
    <T> T lRightPop(String key, Class<T> clazz);

    /**
     * 从右侧弹出 List（支持复杂泛型）
     *
     * @param key     缓存键
     * @param typeRef 返回值类型引用
     * @param <T>     返回值类型
     * @return 值
     */
    <T> T lRightPop(String key, TypeReference<T> typeRef);

    /**
     * 获取 List 范围内的元素
     *
     * @param key   缓存键
     * @param start 开始索引
     * @param end   结束索引
     * @param clazz 返回值类型类
     * @param <T>   返回值类型
     * @return 元素列表
     */
    <T> List<T> lRange(String key, long start, long end, Class<T> clazz);

    /**
     * 获取 List 范围内的元素（支持复杂泛型）
     *
     * @param key     缓存键
     * @param start   开始索引
     * @param end     结束索引
     * @param typeRef 返回值类型引用
     * @param <T>     返回值类型
     * @return 元素列表
     */
    <T> List<T> lRange(String key, long start, long end, TypeReference<T> typeRef);

    /**
     * 获取 List 所有元素（使用 Supplier 加载）
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 元素列表
     */
    <T> List<T> lGetAll(String key, Supplier<List<T>> supplier, Class<T> clazz);

    /**
     * 获取 List 所有元素（使用 Supplier 加载，支持复杂泛型）
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 元素列表
     */
    <T> List<T> lGetAll(String key, Supplier<List<T>> supplier, TypeReference<T> typeRef);

    /**
     * 设置 List 指定索引的值
     *
     * @param key   缓存键
     * @param index 索引
     * @param value 值
     */
    void lSet(String key, long index, Object value);

    /**
     * 获取 List 指定索引的值
     *
     * @param key   缓存键
     * @param index 索引
     * @param clazz 返回值类型类
     * @param <T>   返回值类型
     * @return 值
     */
    <T> T lIndex(String key, long index, Class<T> clazz);

    /**
     * 获取 List 指定索引的值（支持复杂泛型）
     *
     * @param key     缓存键
     * @param index   索引
     * @param typeRef 返回值类型引用
     * @param <T>     返回值类型
     * @return 值
     */
    <T> T lIndex(String key, long index, TypeReference<T> typeRef);

    /**
     * 移除 List 中指定值
     *
     * @param key   缓存键
     * @param count 移除数量（正数从左侧开始，负数从右侧开始，0表示全部）
     * @param value 值
     * @return 移除数量
     */
    long lRemove(String key, long count, Object value);

    /**
     * 获取 List 大小
     *
     * @param key 缓存键
     * @return 元素数量
     */
    long lSize(String key);

    /**
     * 修剪 List
     *
     * @param key   缓存键
     * @param start 开始索引
     * @param end   结束索引
     */
    void lTrim(String key, long start, long end);

    // ==================== Set 操作 ====================

    /**
     * 添加 Set 成员
     *
     * @param key    缓存键
     * @param member 成员
     * @return 是否添加成功（1 表示新增，0 表示已存在）
     */
    long sAdd(String key, Object member);

    /**
     * 批量添加 Set 成员
     *
     * @param key     缓存键
     * @param members 成员集合
     * @return 新增数量
     */
    long sAddAll(String key, Collection<?> members);

    /**
     * 移除 Set 成员
     *
     * @param key    缓存键
     * @param member 成员
     * @return 是否移除成功
     */
    long sRemove(String key, Object member);

    /**
     * 批量移除 Set 成员
     *
     * @param key     缓存键
     * @param members 成员集合
     * @return 移除数量
     */
    long sRemoveAll(String key, Collection<?> members);

    /**
     * 判断是否为 Set 成员
     *
     * @param key    缓存键
     * @param member 成员
     * @return 是否是成员
     */
    boolean sIsMember(String key, Object member);

    /**
     * 获取 Set 所有成员（使用 Supplier 加载）
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 成员集合
     */
    <T> Set<T> sMembers(String key, Supplier<Set<T>> supplier, Class<T> clazz);

    /**
     * 获取 Set 所有成员（使用 Supplier 加载，支持复杂泛型）
     *
     * @param key      缓存键
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 成员集合
     */
    <T> Set<T> sMembers(String key, Supplier<Set<T>> supplier, TypeReference<T> typeRef);

    /**
     * 获取 Set 大小
     *
     * @param key 缓存键
     * @return 成员数量
     */
    long sSize(String key);

    /**
     * 获取 Set 随机成员
     *
     * @param key   缓存键
     * @param clazz 返回值类型类
     * @param <T>   返回值类型
     * @return 随机成员
     */
    <T> T sRandomMember(String key, Class<T> clazz);

    /**
     * 获取 Set 随机成员（支持复杂泛型）
     *
     * @param key     缓存键
     * @param typeRef 返回值类型引用
     * @param <T>     返回值类型
     * @return 随机成员
     */
    <T> T sRandomMember(String key, TypeReference<T> typeRef);

    /**
     * 获取 Set 多个随机成员
     *
     * @param key   缓存键
     * @param count 数量
     * @param clazz 返回值类型类
     * @param <T>   返回值类型
     * @return 随机成员集合
     */
    <T> Set<T> sDistinctRandomMembers(String key, long count, Class<T> clazz);

    /**
     * 获取 Set 多个随机成员（支持复杂泛型）
     *
     * @param key     缓存键
     * @param count   数量
     * @param typeRef 返回值类型引用
     * @param <T>     返回值类型
     * @return 随机成员集合
     */
    <T> Set<T> sDistinctRandomMembers(String key, long count, TypeReference<T> typeRef);

    /**
     * 弹出 Set 随机成员
     *
     * @param key   缓存键
     * @param clazz 返回值类型类
     * @param <T>   返回值类型
     * @return 弹出的成员
     */
    <T> T sPop(String key, Class<T> clazz);

    /**
     * 弹出 Set 随机成员（支持复杂泛型）
     *
     * @param key     缓存键
     * @param typeRef 返回值类型引用
     * @param <T>     返回值类型
     * @return 弹出的成员
     */
    <T> T sPop(String key, TypeReference<T> typeRef);

    /**
     * 获取两个 Set 的交集
     *
     * @param key      缓存键
     * @param otherKey 另一个缓存键
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 交集
     */
    <T> Set<T> sIntersect(String key, String otherKey, Class<T> clazz);

    /**
     * 获取两个 Set 的交集（支持复杂泛型）
     *
     * @param key      缓存键
     * @param otherKey 另一个缓存键
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 交集
     */
    <T> Set<T> sIntersect(String key, String otherKey, TypeReference<T> typeRef);

    /**
     * 获取两个 Set 的并集
     *
     * @param key      缓存键
     * @param otherKey 另一个缓存键
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 并集
     */
    <T> Set<T> sUnion(String key, String otherKey, Class<T> clazz);

    /**
     * 获取两个 Set 的并集（支持复杂泛型）
     *
     * @param key      缓存键
     * @param otherKey 另一个缓存键
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 并集
     */
    <T> Set<T> sUnion(String key, String otherKey, TypeReference<T> typeRef);

    /**
     * 获取两个 Set 的差集
     *
     * @param key      缓存键
     * @param otherKey 另一个缓存键
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 差集
     */
    <T> Set<T> sDifference(String key, String otherKey, Class<T> clazz);

    /**
     * 获取两个 Set 的差集（支持复杂泛型）
     *
     * @param key      缓存键
     * @param otherKey 另一个缓存键
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 差集
     */
    <T> Set<T> sDifference(String key, String otherKey, TypeReference<T> typeRef);

    // ==================== Sorted Set (ZSet) 操作 ====================

    /**
     * 添加 ZSet 成员
     *
     * @param key   缓存键
     * @param value 成员
     * @param score 分数
     * @return 是否添加成功
     */
    boolean zAdd(String key, Object value, double score);

    /**
     * 批量添加 ZSet 成员
     *
     * @param key    缓存键
     * @param tuples 成员-分数映射
     * @return 新增数量
     */
    long zAddAll(String key, Map<Object, Double> tuples);

    /**
     * 移除 ZSet 成员
     *
     * @param key    缓存键
     * @param member 成员
     * @return 是否移除成功
     */
    long zRemove(String key, Object member);

    /**
     * 批量移除 ZSet 成员
     *
     * @param key     缓存键
     * @param members 成员集合
     * @return 移除数量
     */
    long zRemoveAll(String key, Collection<?> members);

    /**
     * 按排名范围移除 ZSet 成员
     *
     * @param key   缓存键
     * @param start 开始排名
     * @param end   结束排名
     * @return 移除数量
     */
    long zRemoveRange(String key, long start, long end);

    /**
     * 按分数范围移除 ZSet 成员
     *
     * @param key 缓存键
     * @param min 最小分数
     * @param max 最大分数
     * @return 移除数量
     */
    long zRemoveRangeByScore(String key, double min, double max);

    /**
     * 增加 ZSet 成员分数
     *
     * @param key   缓存键
     * @param value 成员
     * @param delta 增量
     * @return 新分数
     */
    double zIncrementScore(String key, Object value, double delta);

    /**
     * 获取 ZSet 成员排名（从小到大）
     *
     * @param key    缓存键
     * @param member 成员
     * @return 排名（从 0 开始）
     */
    Long zRank(String key, Object member);

    /**
     * 获取 ZSet 成员排名（从大到小）
     *
     * @param key    缓存键
     * @param member 成员
     * @return 排名（从 0 开始）
     */
    Long zReverseRank(String key, Object member);

    /**
     * 按排名范围获取 ZSet 成员（使用 Supplier 加载）
     *
     * @param key      缓存键
     * @param start    开始排名
     * @param end      结束排名
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 成员集合
     */
    <T> Set<T> zRange(String key, long start, long end, Supplier<Set<T>> supplier, Class<T> clazz);

    /**
     * 按排名范围获取 ZSet 成员（使用 Supplier 加载，支持复杂泛型）
     *
     * @param key      缓存键
     * @param start    开始排名
     * @param end      结束排名
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 成员集合
     */
    <T> Set<T> zRange(String key, long start, long end, Supplier<Set<T>> supplier, TypeReference<T> typeRef);

    /**
     * 按排名范围获取 ZSet 成员和分数（使用 Supplier 加载）
     *
     * @param key      缓存键
     * @param start    开始排名
     * @param end      结束排名
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 成员-分数映射
     */
    <T> Map<T, Double> zRangeWithScores(String key, long start, long end, Supplier<Map<T, Double>> supplier, Class<T> clazz);

    /**
     * 按排名范围获取 ZSet 成员和分数（使用 Supplier 加载，支持复杂泛型）
     *
     * @param key      缓存键
     * @param start    开始排名
     * @param end      结束排名
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 成员-分数映射
     */
    <T> Map<T, Double> zRangeWithScores(String key, long start, long end, Supplier<Map<T, Double>> supplier, TypeReference<T> typeRef);

    /**
     * 按分数范围获取 ZSet 成员（使用 Supplier 加载）
     *
     * @param key      缓存键
     * @param min      最小分数
     * @param max      最大分数
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 成员集合
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, Supplier<Set<T>> supplier, Class<T> clazz);

    /**
     * 按分数范围获取 ZSet 成员（使用 Supplier 加载，支持复杂泛型）
     *
     * @param key      缓存键
     * @param min      最小分数
     * @param max      最大分数
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 成员集合
     */
    <T> Set<T> zRangeByScore(String key, double min, double max, Supplier<Set<T>> supplier, TypeReference<T> typeRef);

    /**
     * 按分数范围获取 ZSet 成员和分数（使用 Supplier 加载）
     *
     * @param key      缓存键
     * @param min      最小分数
     * @param max      最大分数
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 成员-分数映射
     */
    <T> Map<T, Double> zRangeByScoreWithScores(String key, double min, double max, Supplier<Map<T, Double>> supplier, Class<T> clazz);

    /**
     * 按分数范围获取 ZSet 成员和分数（使用 Supplier 加载，支持复杂泛型）
     *
     * @param key      缓存键
     * @param min      最小分数
     * @param max      最大分数
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 成员-分数映射
     */
    <T> Map<T, Double> zRangeByScoreWithScores(String key, double min, double max, Supplier<Map<T, Double>> supplier, TypeReference<T> typeRef);

    /**
     * 按排名范围获取 ZSet 成员（从大到小，使用 Supplier 加载）
     *
     * @param key      缓存键
     * @param start    开始排名
     * @param end      结束排名
     * @param supplier 数据加载器
     * @param clazz    返回值类型类
     * @param <T>      返回值类型
     * @return 成员集合
     */
    <T> Set<T> zReverseRange(String key, long start, long end, Supplier<Set<T>> supplier, Class<T> clazz);

    /**
     * 按排名范围获取 ZSet 成员（从大到小，使用 Supplier 加载，支持复杂泛型）
     *
     * @param key      缓存键
     * @param start    开始排名
     * @param end      结束排名
     * @param supplier 数据加载器
     * @param typeRef  返回值类型引用
     * @param <T>      返回值类型
     * @return 成员集合
     */
    <T> Set<T> zReverseRange(String key, long start, long end, Supplier<Set<T>> supplier, TypeReference<T> typeRef);

    /**
     * 获取 ZSet 成员分数
     *
     * @param key    缓存键
     * @param member 成员
     * @return 分数
     */
    Double zScore(String key, Object member);

    /**
     * 获取 ZSet 大小
     *
     * @param key 缓存键
     * @return 成员数量
     */
    long zSize(String key);

    /**
     * 获取 ZSet 分数范围内成员数量
     *
     * @param key 缓存键
     * @param min 最小分数
     * @param max 最大分数
     * @return 成员数量
     */
    long zCount(String key, double min, double max);

    // ==================== Keys 操作 ====================

    /**
     * 按模式匹配获取所有 key
     *
     * @param pattern 匹配模式（如 "user:*"）
     * @return key 集合
     */
    Set<String> keys(String pattern);

    /**
     * 扫描获取 key（推荐用于生产环境）
     *
     * @param pattern 匹配模式
     * @param count   每次扫描数量
     * @return key 集合
     */
    Set<String> scan(String pattern, long count);

    /**
     * 删除匹配模式的所有 key
     *
     * @param pattern 匹配模式
     * @return 删除数量
     */
    long deleteByPattern(String pattern);

    /**
     * 获取 key 总数
     *
     * @return key 数量
     */
    long dbSize();

    /**
     * 清空当前数据库
     */
    void flushDb();

    /**
     * 清空所有数据库
     */
    void flushAll();

    // ==================== 分布式锁 ====================

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey, long timeout, TimeUnit unit);

    /**
     * 释放分布式锁
     *
     * @param lockKey 锁键
     * @return 是否释放成功
     */
    boolean unlock(String lockKey);

    /**
     * 使用分布式锁执行操作
     *
     * @param lockKey  锁键
     * @param timeout  超时时间
     * @param unit     时间单位
     * @param supplier 执行的操作
     * @param <T>      返回值类型
     * @return 操作结果
     */
    <T> T withLock(String lockKey, long timeout, TimeUnit unit, Supplier<T> supplier);

}

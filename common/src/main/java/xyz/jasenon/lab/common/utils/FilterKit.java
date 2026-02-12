package xyz.jasenon.lab.common.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class FilterKit {

    /**
     * 返回一个谓词(Predicate)，用于根据指定键提取器函数对流中的元素进行去重
     *
     * @param <T> 输入元素的类型
     * @param keyExtractor 键提取器函数，用于从元素中提取用于比较的键
     * @return 一个谓词，当且仅当元素的关键字是第一次出现时返回true
     *
     * @apiNote 这个方法可以用于Stream的filter操作，实现基于某个属性的去重
     * @implNote 使用ConcurrentHashMap来保证线程安全，适合并行流处理
     *
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

}

package xyz.jasenon.lab.class_time_table.t_io.protocol;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 序列号生成器
 * 用于为数据包生成唯一的序列号，支持QoS确认机制
 * 
 * @author Jasenon_ce
 * @date 2026/1/31
 */
public class SeqIdGenerator {
    
    /** 序列号从1开始，0保留为无效值 */
    private static final int INITIAL_SEQ = 1;
    
    /** 序列号最大值（Short的最大值） */
    private static final int MAX_SEQ = Short.MAX_VALUE;
    
    /** 原子计数器，保证线程安全 */
    private final AtomicInteger counter;
    
    /**
     * 构造函数，从1开始
     */
    public SeqIdGenerator() {
        this.counter = new AtomicInteger(INITIAL_SEQ);
    }
    
    /**
     * 构造函数，指定初始值
     * 
     * @param initialValue 初始值
     */
    public SeqIdGenerator(int initialValue) {
        if (initialValue < 0 || initialValue > MAX_SEQ) {
            throw new IllegalArgumentException("初始值必须在0-" + MAX_SEQ + "之间");
        }
        this.counter = new AtomicInteger(initialValue);
    }
    
    /**
     * 生成下一个序列号
     * 
     * @return 序列号（Short类型）
     */
    public short nextSeqId() {
        int next = counter.getAndIncrement();
        
        // 如果超过最大值，重置为1（避免溢出）
        if (next > MAX_SEQ) {
            synchronized (this) {
                // 双重检查，避免并发问题
                int current = counter.get();
                if (current > MAX_SEQ) {
                    counter.set(INITIAL_SEQ);
                    next = INITIAL_SEQ;
                } else {
                    next = counter.getAndIncrement();
                }
            }
        }
        
        return (short) next;
    }
    
    /**
     * 获取当前序列号（不递增）
     * 
     * @return 当前序列号
     */
    public short currentSeqId() {
        int current = counter.get();
        if (current > MAX_SEQ) {
            return INITIAL_SEQ;
        }
        return (short) current;
    }
    
    /**
     * 重置序列号
     */
    public void reset() {
        counter.set(INITIAL_SEQ);
    }
    
    /**
     * 重置序列号为指定值
     * 
     * @param value 新的序列号值
     */
    public void reset(int value) {
        if (value < 0 || value > MAX_SEQ) {
            throw new IllegalArgumentException("序列号值必须在0-" + MAX_SEQ + "之间");
        }
        counter.set(value);
    }
}



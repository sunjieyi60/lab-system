package xyz.jasenon.lab.mqtt.mqtt;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Function;

@Slf4j
public class PriorityBlockingQueueWithCondition<T> {

    private final PriorityBlockingQueue<T> queue = new PriorityBlockingQueue<>();
    private final ConcurrentHashMap<Object,Boolean> flag = new ConcurrentHashMap<>();
    private final Function<T,Boolean> checker;
    private final Function<T,Object> keyer;

    public PriorityBlockingQueueWithCondition(Function<T,Boolean> checker,Function<T,Object> keyer){
        this.checker = checker;
        this.keyer = keyer;
    }

    public boolean add(T value){
        if (checker.apply(value)){
            Object key = keyer.apply(value);
            if (!flag.getOrDefault(key,false)){
                flag.put(key,true);
                boolean added = queue.add(value);
                log.debug("POLLING任务添加成功，key: {}, queueSize: {}, flagSize: {}", key, queue.size(), flag.size());
                return added;
            }
            log.debug("POLLING任务添加失败（flag已存在），key: {}, queueSize: {}, flagSize: {}", key, queue.size(), flag.size());
            return false;
        }
        return queue.add(value);
    }

    /**
     * 从队列中取出任务，如果是需要检查的任务，同时移除其flag
     * 因为任务已经从队列中取出，flag应该被清理
     */
    public T poll(){
        T task = queue.poll();
        if (task != null && checker.apply(task)) {
            Object key = keyer.apply(task);
            flag.remove(key);
            log.debug("POLLING任务从队列取出，移除flag，key: {}, queueSize: {}, flagSize: {}", key, queue.size(), flag.size());
        }
        return task;
    }
    
    /**
     * 移除指定任务的flag（在任务成功发送后调用，但poll时已经移除了，这里作为备用）
     * @param task 任务对象
     */
    public void removeFlag(T task) {
        if (task != null && checker.apply(task)) {
            flag.remove(keyer.apply(task));
        }
    }

    public boolean isEmpty(){
        return queue.isEmpty();
    }

}

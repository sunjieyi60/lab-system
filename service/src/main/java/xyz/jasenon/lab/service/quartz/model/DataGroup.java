package xyz.jasenon.lab.service.quartz.model;

import com.baomidou.mybatisplus.annotation.TableField;

import java.util.List;

/**
 * @author Jasenon_ce
 * @date 2026/1/4
 */
public class DataGroup {
    /**
     * 数据组ID
     */
    private String id;

    /**
     * 任务ID
     */
    private String scheduleTaskId;

    /**
     * 数据源
     */
    @TableField(exist = false)
    private List<Data> datas;
}

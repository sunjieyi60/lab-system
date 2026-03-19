package xyz.jasenon.lab.class_time_table.vo;

import lombok.Data;
import xyz.jasenon.lab.core.packets.BitmapRespBody;

import java.util.List;

/**
 * 人脸下发结果VO
 */
@Data
public class BitmapPushResultVo {

    /** 总设备数 */
    private int total;

    /** 成功数量 */
    private int success;

    /** 失败数量 */
    private int fail;

    /** 离线数量 */
    private int offline;

    /** 详细结果列表 */
    private List<BitmapRespBody> details;
}

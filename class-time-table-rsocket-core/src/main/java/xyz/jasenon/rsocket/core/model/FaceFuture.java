package xyz.jasenon.rsocket.core.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @description 人脸特征及其对应的名称
 */
@Getter
@Setter
public class FaceFuture {

    /**
     * 人脸位图
     */
    private byte[] bitmap;

    /**
     * 人脸名称
     */
    private String faceName;

}

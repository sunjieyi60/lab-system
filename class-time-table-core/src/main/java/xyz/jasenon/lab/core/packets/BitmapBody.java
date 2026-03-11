package xyz.jasenon.lab.core.packets;

import lombok.Getter;

@Getter
public class BitmapBody extends Message {

    /**
     * bitmap 图片位图
     */
    private byte[] bitmap;

    /**
     * 人脸名称
     */
    private String faceName;

    public BitmapBody(){}

    public BitmapBody(byte[] bitmap, String faceName){
        this.bitmap = bitmap;
        this.faceName = faceName;
    }

}

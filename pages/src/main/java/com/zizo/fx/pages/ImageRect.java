package com.zizo.fx.pages;

public class ImageRect{
    public float iHeight;
    public float iWidth;

    public ImageRect(float height,float width){
        iHeight = height;
        iWidth = width;
    }

    public float getZoom(float displayHeight,float displayWidth){
        float minDisplay = Math.min(displayHeight,displayWidth);
        float minImage = Math.min(iHeight,iWidth);

        float minPercent = minDisplay / minImage;

        return minPercent;
    }
}

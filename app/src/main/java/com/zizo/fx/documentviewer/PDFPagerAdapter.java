package com.zizo.fx.documentviewer;

import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import com.zizo.fx.pages.ImageRect;
import com.zizo.fx.pages.PDFPages;

import uk.co.senab.photoview.PhotoView;

/**
 * PDF翻页适配器-用于绑定pdf数据
 * 由 XMM 于 2015-01-06.创建
 */
class PDFPagerAdapter extends PagerAdapter {

    //pdf文件数据
    private PDFPages mPDFPages;

    public void setPdf(PDFPages pdfPages){
        mPDFPages = pdfPages;
    }

    //获得页数
    @Override
    public int getCount() {
        return mPDFPages.getPageCount();
    }

    //初始化页
    @Override
    public View instantiateItem(ViewGroup container, int position) {
        //创建图片视图-继承与ImageView
        PhotoView photoView = new PhotoView(container.getContext());
        //获得第position+1页的长宽数据
        ImageRect rect = mPDFPages.getRect(position + 1);
        //获得屏幕长宽参数
        DisplayMetrics mDisplayMetrics = container.getResources().getDisplayMetrics();
        //计算满屏显示的放大比例
        float zoom = rect.getZoom(mDisplayMetrics.heightPixels,mDisplayMetrics.widthPixels);
        //获得渲染图
        //mPDFPages.getBitMap(position + 1,zoom);
        //固定使用3倍放大图
        Bitmap bi = mPDFPages.getBitMap(position + 1,3);
        //渲染该页
        photoView.setImageBitmap(bi);

        // 附加视图
        container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        return photoView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

}

package com.zizo.fx.documentviewer;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.EventListener;

/**
 * 由 XMM 于 2015-01-05.创建
 */
public class PDFViewPager extends ViewPager {

    OnInterceptTouch mOnInterceptTouchEvent;

    public PDFViewPager(Context context) {
        super(context);
    }

    public PDFViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnInterceptTouchEvent(OnInterceptTouch onInterceptTouchEvent){
        mOnInterceptTouchEvent = onInterceptTouchEvent;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(mOnInterceptTouchEvent!=null) {
            if(mOnInterceptTouchEvent.onInterceptTouch(this,ev)){
                return true;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

}

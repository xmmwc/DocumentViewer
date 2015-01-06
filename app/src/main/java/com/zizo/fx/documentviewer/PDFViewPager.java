package com.zizo.fx.documentviewer;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by XMM on 2015-01-05.
 */
public class PDFViewPager extends ViewPager {
    private OnTouchListener mOtl;

    public PDFViewPager(Context context) {
        super(context);
    }

    public PDFViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnTouch(OnTouchListener otl){
        mOtl = otl;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev){
        if(mOtl!=null) mOtl.onTouch(this,ev);
        return super.onTouchEvent(ev);
    }
}

package com.zizo.fx.documentviewer;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 由 XMM 于 2015-01-05.创建
 */
public class PDFViewPager extends ViewPager {

    //private float initialX,initialY;
    //private OnClickListener mClickListener;

    public PDFViewPager(Context context) {
        super(context);
    }

    public PDFViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

//    public void setClick(OnClickListener clickListener){
//        mClickListener = clickListener;
//    }

//    private boolean inRange(float finalX,float finalY) {
//        int rc = 10;
//        return initialX < finalX + rc && initialX > finalX - rc
//                && initialY < finalY + rc && initialY > finalY - rc;
//    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev){
//        int action = ev.getActionMasked();
//        float finalX = 0,finalY = 0;
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                initialX = ev.getX();
//                initialY = ev.getY();
//                break;
//            case MotionEvent.ACTION_UP:
//                finalX = ev.getX();
//                finalY = ev.getY();
//                break;
//        }
//        if(inRange(finalX,finalY)){
//            if(mClickListener !=null){
//                mClickListener.onClick(this);
//                return true;
//            }
//        }
//        return super.dispatchTouchEvent(ev);
//    }
}

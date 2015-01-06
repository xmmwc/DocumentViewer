package com.zizo.fx.documentviewer;

import android.view.MotionEvent;
import android.view.View;

public interface OnInterceptTouch {
    boolean onInterceptTouch(View v, MotionEvent event);
}

package com.zizo.fx.documentviewer;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.sun.pdfview.decrypt.PDFAuthenticationFailureException;
import com.zizo.fx.pages.ImageRect;
import com.zizo.fx.pages.PDFPages;


import uk.co.senab.photoview.PhotoView;


public class PdfViewActivity extends ActionBarActivity {

    private static final String Tag = "self";
    //pdf文件地址
    public  static final String mPdfPath = "";
    //pdf翻页对象
    private PDFViewPager mViewPager;
    //pdf文件数据
    private PDFPages mPDFPage;

    private float mControlsHeight = 0;
    private boolean isVisible;
    private FrameLayout mControls;

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        Log.i(Tag, "准备还原数据");
        return this;
    }

    private boolean restoreInstance() {
        if (getLastCustomNonConfigurationInstance()==null)
            return false;
        PdfViewActivity inst =(PdfViewActivity)getLastCustomNonConfigurationInstance();
        if (inst != this) {
            Log.i(Tag, "正在还原数据");
            mPDFPage = inst.mPDFPage;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_view);
        mViewPager = (PDFViewPager)findViewById(R.id.view_pager);

        setSeekBar();

        //恢复数据
        restoreInstance();

        //如果数据还在直接渲染
        if(mPDFPage!=null){
            PDFPagerAdapter mPDFPagerAdapter = new PDFPagerAdapter();
            mPDFPagerAdapter.setPdf(mPDFPage);
            mViewPager.setAdapter(mPDFPagerAdapter);
        }else{
            mPDFPage = new PDFPages(){
                //全部页数据加载完
                @Override
                public void afterLoadPages(){
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            PDFPagerAdapter mPDFPagerAdapter = new PDFPagerAdapter();
                            mPDFPagerAdapter.setPdf(mPDFPage);
                            mViewPager.setAdapter(mPDFPagerAdapter);
                        }
                    });
                }
            };
            String pdfPath = getIntent().getStringExtra(PdfViewActivity.mPdfPath);
            try {
                //尝试打开pdf文件
                mPDFPage.tryToOpenPdf(pdfPath,null);
            } catch (PDFAuthenticationFailureException e) {
                e.printStackTrace();
                Log.e(Tag, e.getMessage(), e);
                //todo：如果打不开提示输入密码
            }
            //开启线程加载pdf数据
            mPDFPage.loadPagesByThread();
        }
    }

    private void setSeekBar(){
        mControls = (FrameLayout)findViewById(R.id.seek_layout);
        FrameLayout viewContainer = (FrameLayout) findViewById(R.id.view_container);
        toggleSeekBar(true);

        mViewPager.setOnTouch(new View.OnTouchListener() {
            private boolean isMove = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_MOVE:
                        isMove = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!isMove){
                            toggleSeekBar(isVisible);
                            return true;
                        }
                        break;
                }

                return true;
            }
        });
    }

    private void toggleSeekBar(boolean visible){
        isVisible = !isVisible;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            // If the ViewPropertyAnimator API is available
            // (Honeycomb MR2 and later), use it to animate the
            // in-layout UI controls at the bottom of the
            // screen.
            if (mControlsHeight == 0) {
                mControlsHeight = mControls.getHeight();
            }
            mControls.animate()
                    .translationY(visible ? 0 : mControlsHeight);
        } else {
            // If the ViewPropertyAnimator APIs aren't
            // available, simply show or hide the in-layout UI
            // controls.
            mControls.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pdf_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    static class PDFPagerAdapter extends PagerAdapter {

        //Map<Integer,PhotoView> mPhotoView;
        private PDFPages mPDFPages;

        public void setPdf(PDFPages pdfPages){
            mPDFPages = pdfPages;
        }

        @Override
        public int getCount() {
            return mPDFPages.getPageCount();
        }

        @Override
        public View instantiateItem(ViewGroup container, int position) {
            PhotoView photoView = new PhotoView(container.getContext());
            //获得第position+1页的长宽数据
            ImageRect rect = mPDFPages.getRect(position + 1);
            //获得屏幕长宽参数
            DisplayMetrics mDisplayMetrics = container.getResources().getDisplayMetrics();
            //计算满屏显示的放大比例
            float zoom = rect.getZoom(mDisplayMetrics.heightPixels,mDisplayMetrics.widthPixels);
            //获得渲染图
            Bitmap bi = mPDFPages.getBitMap(position + 1,zoom);
            //渲染该页
            photoView.setImageBitmap(bi);

            // Now just add PhotoView to ViewPager and return it
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}

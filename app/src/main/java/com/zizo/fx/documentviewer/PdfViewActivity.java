package com.zizo.fx.documentviewer;

import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.sun.pdfview.decrypt.PDFAuthenticationFailureException;
import com.zizo.fx.pages.PDFPages;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


public class PdfViewActivity extends ActionBarActivity {

    private static final String Tag = "self";
    //pdf文件地址
    public  static final String mPdfPath = "";
    //pdf翻页对象
    private PDFViewPager mViewPager;
    //pdf文件数据
    private PDFPages mPDFPage;

    private File mTmpFile;

    //private float mControlsHeight = 0;
    //private boolean isVisible;
    //private FrameLayout mControls;

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

        //todo:setSeekBar();

        //恢复数据
        restoreInstance();

        //如果数据还在直接渲染
        if(mPDFPage!=null){
            PDFPagerAdapter mPDFPagerAdapter = new PDFPagerAdapter();
            mPDFPagerAdapter.setPdf(mPDFPage);
            mViewPager.setAdapter(mPDFPagerAdapter);
        }else{
            mPDFPage = new PDFPages(this){
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
            pdfPath = storeUriContentToFile(getIntent().getData());
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

    private String storeUriContentToFile(Uri uri) {
        String result = null;
        try {
            if (mTmpFile == null) {
                File root = Environment.getExternalStorageDirectory();
                if (root == null)
                    throw new Exception("external storage dir not found");
                mTmpFile = new File(root,"DocumentViewer/document_viewer_temp.pdf");
                mTmpFile.getParentFile().mkdirs();
                mTmpFile.delete();
            }
            else {
                mTmpFile.delete();
            }
            InputStream is = getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(mTmpFile);
            byte[] buf = new byte[1024];
            int cnt = is.read(buf);
            while (cnt > 0) {
                os.write(buf, 0, cnt);
                cnt = is.read(buf);
            }
            os.close();
            is.close();
            result = mTmpFile.getCanonicalPath();
            mTmpFile.deleteOnExit();
        }
        catch (Exception e) {
            Log.e(Tag, e.getMessage(), e);
        }
        return result;
    }

//    private void setSeekBar(){
//        mControls = (FrameLayout)findViewById(R.id.seek_layout);
//        toggleSeekBar(true);
//        mViewPager.setClick(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                toggleSeekBar(isVisible);
//            }
//        });
//    }

//    private void toggleSeekBar(boolean visible){
//        isVisible = !isVisible;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
//            // If the ViewPropertyAnimator API is available
//            // (Honeycomb MR2 and later), use it to animate the
//            // in-layout UI controls at the bottom of the
//            // screen.
//            if (mControlsHeight == 0) {
//                mControlsHeight = mControls.getHeight();
//            }
//            mControls.animate()
//                    .translationY(visible ? 0 : mControlsHeight);
//        } else {
//            // If the ViewPropertyAnimator APIs aren't
//            // available, simply show or hide the in-layout UI
//            // controls.
//            mControls.setVisibility(visible ? View.VISIBLE : View.GONE);
//        }
//    }

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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}

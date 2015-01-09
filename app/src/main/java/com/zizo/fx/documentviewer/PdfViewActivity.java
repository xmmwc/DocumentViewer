package com.zizo.fx.documentviewer;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.zizo.fx.pages.PDFPages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class PdfViewActivity extends Activity{

    private static final String Tag = "PdfViewActivity";
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
    public Object onRetainNonConfigurationInstance() {
        Log.i(Tag, "准备还原数据");
        return this;
    }

    private boolean restoreInstance() {
        if (getLastNonConfigurationInstance()==null)
            return false;
        PdfViewActivity inst =(PdfViewActivity)getLastNonConfigurationInstance();
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

        String pdfPath = getIntent().getStringExtra(PdfViewActivity.mPdfPath);

        tryLoadPdf(pdfPath);
    }

    private void tryLoadPdf(String url){
        if (mPDFPage==null) {
            new StoreHttpAsyncTask().execute(url);
        }else {
            setPdfView();
        }
    }

    private void setPdfView(){
        try {
            PDFPagerAdapter mPDFPagerAdapter = new PDFPagerAdapter();
            mPDFPagerAdapter.setPdf(mPDFPage);
            mViewPager.setAdapter(mPDFPagerAdapter);
        } catch (Throwable e) {
            Log.e(Tag, e.getMessage(), e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String storePdfToFile(String urlPath) throws Exception {
        String result = null;
        URL url = new URL(urlPath);
        HttpURLConnection urlConnection = null;
        try {
            if (mTmpFile == null) {
                File root = Environment.getExternalStorageDirectory();
                if (root == null)
                    throw new Exception("没有找到存储空间");
                mTmpFile = new File(root, "DocumentViewer/document_viewer_temp.pdf");
                if(mTmpFile.getParentFile().mkdirs())
                    Log.i(Tag,"创建了文件夹：" + mTmpFile.getParentFile().getPath());
                if(mTmpFile.delete())
                    Log.i(Tag,"删除了文件：" + mTmpFile.getPath());
            } else {
                if(mTmpFile.delete())
                    Log.i(Tag,"删除了文件：" + mTmpFile.getPath());
            }

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream is = urlConnection.getInputStream();

            int responseCode = urlConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
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
            } else {
                throw new Exception("网络连接失败");
            }
        }catch (FileNotFoundException e){
            assert urlConnection != null;
            InputStream is = urlConnection.getErrorStream();
            BufferedReader theReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String reply;
            while ((reply = theReader.readLine()) != null) {
                response.append(reply);
            }
            String ErrorMsg = response.toString();
            Log.e(Tag,ErrorMsg);
        } catch (Exception e) {
            Log.e(Tag, e.getMessage(), e);
            throw e;
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

    private class StoreHttpAsyncTask extends AsyncTask<String,Void,PDFPages> {

        private String Tag = "StoreHttpAsyncTask";

        @Override
        protected PDFPages doInBackground(String... urls) {
            try {
                String pdfPath = storePdfToFile(urls[0]);
                mPDFPage = new PDFPages();
                mPDFPage.tryToOpenPdf(pdfPath, null);
                mPDFPage.loadPages();
            } catch (Exception e) {
                Log.e(Tag, e.getMessage(), e);
                e.printStackTrace();
//                if ((e.getClass()).equals(PDFAuthenticationFailureException.class)) {
//                    //todo:输入密码
//                }
                cancel(true);
            }
            return mPDFPage;
        }

        @Override
        protected  void onPostExecute(PDFPages PDFFile) {
            if (PDFFile != null)
                setPdfView();
        }

        @Override
        protected void onCancelled(){
            Toast.makeText(PdfViewActivity.this, "PDF加载失败", Toast.LENGTH_SHORT).show();
        }
    }
}

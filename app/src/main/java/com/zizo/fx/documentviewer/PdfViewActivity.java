package com.zizo.fx.documentviewer;

import android.app.Activity;
import android.app.ProgressDialog;
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


public class PdfViewActivity extends Activity {

    private static final String Tag = "PdfViewActivity";
    //pdf文件地址
    public static final String mPdfPath = "";
    //pdf翻页对象
    private PDFViewPager mViewPager;

    //pdf文件数据
    private PDFPages mPDFPage;

    private File mTmpFile;

    //private float mControlsHeight = 0;
    //private boolean isVisible;
    //private FrameLayout mControls;

    //窗体即将重建
    @Override
    public Object onRetainNonConfigurationInstance() {
        Log.i(Tag, "准备还原数据");
        return this;
    }

    //重建窗体
    private boolean restoreInstance() {
        if (getLastNonConfigurationInstance() == null)
            return false;
        PdfViewActivity inst = (PdfViewActivity) getLastNonConfigurationInstance();
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
        mViewPager = (PDFViewPager) findViewById(R.id.view_pager);
        //todo:setSeekBar();

        //恢复数据
        restoreInstance();

        //获得pdf地址
        String pdfPath = getIntent().getStringExtra(PdfViewActivity.mPdfPath);

        //加载pdf
        tryLoadPdf(pdfPath);
    }

    //试着加载pdf
    private void tryLoadPdf(String url) {
        //如果没有打开过并载入过pdf数据
        if (mPDFPage == null) {
            //开启加载pdf异步任务
            new LoadPDFAsyncTask().execute(url);
        } else {
            setPdfView();
        }
    }

    //绑定pdf数据到视图
    private void setPdfView() {
        try {
            PDFPagerAdapter mPDFPagerAdapter = new PDFPagerAdapter();
            mPDFPagerAdapter.setPdf(mPDFPage);
            mViewPager.setAdapter(mPDFPagerAdapter);
        } catch (Throwable e) {
            Log.e(Tag, e.getMessage(), e);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //将pdf缓存到本地-urlPath为网络地址
    private String storePdfToFile(String urlPath) throws Exception {
        String result = null;
        URL url = new URL(urlPath);

        HttpURLConnection urlConnection = null;
        try {
            if (mTmpFile == null) {
                //系统存储空间目录
                File root = Environment.getExternalStorageDirectory();
                if (root == null)
                    throw new Exception("没有找到存储空间");
                //缓存文件及地址
                mTmpFile = new File(root, "DocumentViewer/document_viewer_temp.pdf");
                //创建缓存文件上级文件夹
                if (mTmpFile.getParentFile().mkdirs())
                    Log.i(Tag, "创建了文件夹：" + mTmpFile.getParentFile().getPath());
                //删除已存在的缓存文件
                if (mTmpFile.delete())
                    Log.i(Tag, "删除了文件：" + mTmpFile.getPath());
            } else {
                if (mTmpFile.delete())
                    Log.i(Tag, "删除了文件：" + mTmpFile.getPath());
            }

            //创建http连接
            urlConnection = (HttpURLConnection) url.openConnection();
            //设置已GET方式获取
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream is = urlConnection.getInputStream();

            //服务器返回代码
            int responseCode = urlConnection.getResponseCode();

            //如果成功（200）
            if (responseCode == HttpURLConnection.HTTP_OK) {
                //写入数据
                OutputStream os = new FileOutputStream(mTmpFile);
                byte[] buf = new byte[1024];
                int cnt = is.read(buf);
                while (cnt > 0) {
                    os.write(buf, 0, cnt);
                    cnt = is.read(buf);
                }
                os.close();
                is.close();
                //获得文件地址
                result = mTmpFile.getCanonicalPath();
                mTmpFile.deleteOnExit();
            } else {
                throw new Exception("网络连接失败");
            }
            //如果服务端没有找到文件
        } catch (FileNotFoundException e) {
            assert urlConnection != null;
            InputStream is = urlConnection.getErrorStream();
            BufferedReader theReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String reply;
            while ((reply = theReader.readLine()) != null) {
                response.append(reply);
            }
            result = response.toString();
            Log.e(Tag, result, e);
            throw new FileNotFoundException(result);
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

    //加载pdf异步任务返回值类
    private class LoadPDFAsyncTaskResult {
        private PDFPages data;
        private Exception error;
    }

    //加载pdf异步任务
    private class LoadPDFAsyncTask extends AsyncTask<String, Void, LoadPDFAsyncTaskResult> {

        private String Tag = "LoadPDFAsyncTask";
        //等待窗
        private ProgressDialog progress;

        @Override
        protected LoadPDFAsyncTaskResult doInBackground(String... urls) {
            LoadPDFAsyncTaskResult result = new LoadPDFAsyncTaskResult();
            try {
                //缓存文件并转换路径
                String pdfPath = storePdfToFile(urls[0]);
                PDFPages pdfPage = new PDFPages();
                //验证pdf文件并打开
                pdfPage.tryToOpenPdf(pdfPath, null);
                //读取所有页（耗时比较长）
                pdfPage.loadPages();
                //返回pdf数据对象
                result.data = pdfPage;
            } catch (Exception e) {
                Log.e(Tag, e.getMessage(), e);
//                if ((e.getClass()).equals(PDFAuthenticationFailureException.class)) {
//                    //todo:输入密码
//                }
                //返回错误
                result.error = e;
                //取消任务
                cancel(true);
            }
            return result;
        }

        @Override
        protected void onPreExecute(){
            progress = ProgressDialog.show(PdfViewActivity.this,"PDF加载中","PDF正努力加载中...");
        }

        @Override
        protected void onPostExecute(LoadPDFAsyncTaskResult result) {
            mPDFPage = result.data;
            progress.dismiss();
            setPdfView();
        }

        @Override
        protected void onCancelled(LoadPDFAsyncTaskResult result) {
            progress.dismiss();
            if (result.error != null)
                Toast.makeText(PdfViewActivity.this, result.error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

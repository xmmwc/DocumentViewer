package com.zizo.fx.documentviewer;

import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.sun.pdfview.decrypt.PDFAuthenticationFailureException;
import com.zizo.fx.filelistgetter.FileList;
import com.zizo.fx.filelistgetter.HttpAsyncTask;
import com.zizo.fx.pages.PDFPages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


public class PdfViewActivity extends ActionBarActivity {

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

        String pdfPath = getIntent().getStringExtra(PdfViewActivity.mPdfPath);

        tryLoadPdf(pdfPath);
    }

    private void tryLoadPdf(String url){
        try {
            new StoreHttpAsyncTask(){
                @Override
                public void onSuccessEvent(final String pdfPath) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            loadPdf(pdfPath);
                        }
                    });
                }

                @Override
                public void onErrorEvent(final Exception e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }.execute(url);
        }catch (Throwable e){
            Log.e(Tag,e.getMessage(),e);
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPdf(String path) {
        //如果数据还在直接渲染
        if (mPDFPage != null) {
            PDFPagerAdapter mPDFPagerAdapter = new PDFPagerAdapter();
            mPDFPagerAdapter.setPdf(mPDFPage);
            mViewPager.setAdapter(mPDFPagerAdapter);
        } else {
            mPDFPage = new PDFPages() {
                //全部页数据加载完
                @Override
                public void afterLoadPages() {
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
            try {
                //尝试打开pdf文件
                mPDFPage.tryToOpenPdf(path, null);
            } catch (PDFAuthenticationFailureException e) {
                e.printStackTrace();
                Log.e(Tag, e.getMessage(), e);
                //todo：如果打不开提示输入密码
            }
            //开启线程加载pdf数据
            mPDFPage.loadPagesByThread();
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
                boolean isMake = mTmpFile.getParentFile().mkdirs();

                mTmpFile.delete();
            } else {
                mTmpFile.delete();
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

    private class StoreHttpAsyncTask extends HttpAsyncTask<String,Exception>{

        private String Tag = "StoreHttpAsyncTask";

        @Override
        protected String doInBackground(String... urls) {
            try {
                String pdfPath = storePdfToFile(urls[0]);
                onSuccessEvent(pdfPath);
                return pdfPath;
            } catch (Exception e) {
                Log.e(Tag, e.getMessage(), e);
                e.printStackTrace();
                onErrorEvent(e);
            }
            return null;
        }

        @Override
        public void onSuccessEvent(String sp) {

        }

        @Override
        public void onErrorEvent(Exception ep) {

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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}

package com.zizo.fx.pages;

import android.graphics.Bitmap;
import android.util.Log;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPaint;
import com.sun.pdfview.decrypt.PDFAuthenticationFailureException;
import com.sun.pdfview.decrypt.PDFPassword;
import com.sun.pdfview.font.PDFFont;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.refs.HardReference;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * 由 XMM 于 2014-12-31.创建
 */
public class PDFPages{
    private static final String Tag = "PDFPages";
    private static final float MIN_ZOOM = 0.25f;//最小放大倍数
    private static final float MAX_ZOOM = 3.0f;//最大放大倍数
    private static final int PAGESTART =1;

    private PDFFile pdfFile;//pdf文件对象
    private int pdfPageCount;//pdf文件页数
    private Map<Integer,PDFPage>  pdfPages;//读取的pdf数据

    private Thread readThread;

    public PDFPages() {
        PDFImage.sShowImages = true;
        PDFPaint.s_doAntiAlias = true;
        PDFFont.sUseFontSubstitution= true;
        HardReference.sKeepCaches= true;
        pdfPages = new HashMap<>();
    }

    public void tryToOpenPdf(String pdfPath,String password) throws PDFAuthenticationFailureException{
        try {
            File f = new File(pdfPath);
            long len = f.length();
            if (len == 0) {
                Log.w(Tag,"这是个空文件");
            }
            else {
                Log.i(Tag,"尝试加载文件，文件大小：" + len + "bytes");
                openPdf(f, password);
            }
        }
        catch (PDFAuthenticationFailureException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            Log.e(Tag,e.getMessage(),e);
        }
    }

    private void openPdf(File file,String password) throws IOException{
        RandomAccessFile raf = new RandomAccessFile(file, "r");//测试随机密码打开

        FileChannel channel = raf.getChannel();

        ByteBuffer bb =
                ByteBuffer.NEW(channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()));

        if (password == null)
            pdfFile = new PDFFile(bb);
        else
            pdfFile = new PDFFile(bb, new PDFPassword(password));

        pdfPageCount = pdfFile.getNumPages();
    }

    public synchronized void loadPagesByThread(){
        if(readThread !=null) return;

        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    loadPages();
                }catch (Throwable e){
                    Log.e(Tag,e.getMessage(),e);
                }
                readThread = null;
            }
        });

        readThread.start();
    }

    public void loadPages() throws Exception {
        for (int i = PAGESTART; i <= pdfPageCount ; i++) {
            loadPage(i);
        }
        afterLoadPages();
    }

    private void loadPage(int pageIndex)throws Exception {
        if (pdfFile == null) {
            Log.e(Tag, "pdf文件还没加载");
            return;
        }
        PDFPage pdfPage = pdfFile.getPage(pageIndex, true);
        pdfPages.put(pageIndex, pdfPage);

        afterLoadPage(pageIndex);
    }

    public void afterLoadPages() {
        Log.i(Tag,"已加载pdf文件所有页面");
    }

    public void afterLoadPage(int pageIndex){
        Log.i(Tag,"已加载pdf文件第" + pageIndex + "页");
    }

    public Bitmap getBitMap(int pageIndex,float zoom) {
        try {
            PDFPage pdfPage = pdfPages.get(pageIndex);

            float width = pdfPage.getWidth();
            float height = pdfPage.getHeight();
            float useZoom = 3;//getZoom(zoom);

            return pdfPage.getImage((int) (width * useZoom), (int) (height * useZoom), null, true, true);
        } catch (Throwable e) {
            Log.e(Tag, e.getMessage(), e);
        }
        return null;
    }

    private float getZoom(float zoom){
        float outZoom = zoom;
        if (zoom<=MIN_ZOOM)
            outZoom = MIN_ZOOM;
        if (zoom>=MAX_ZOOM)
            outZoom = MAX_ZOOM;
        return outZoom;
    }

    public boolean isFullLoaded(){
        return pdfPageCount == pdfPages.size();
    }

    public boolean isLoaded(int pageIndex){
        try {
            return pdfPages.get(pageIndex) != null;
        }catch (Throwable e){
            Log.w(Tag,"第" + pageIndex + "页还没有加载",e);
        }
        return false;
    }

    public int getPageCount(){
        return pdfPageCount;
    }

    public ImageRect getRect(int pageIndex){
        PDFPage pdfPage = pdfPages.get(pageIndex);
        return new ImageRect(pdfPage.getHeight(),pdfPage.getWidth());
    }
}


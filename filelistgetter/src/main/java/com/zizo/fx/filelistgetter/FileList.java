package com.zizo.fx.filelistgetter;


import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 由 XMM 于 2015-01-07.创建
 */
public class FileList {
    private final String TAG = "FileList";

    public void get(String url){
        new FileListAsyncTask().execute(url);
    }

    public void success(JSONArray data){}

    public void error(String errorMsg){}

    private class FileListAsyncTask extends AsyncTask<String,Void,JSONArray>{

        private String errorMsg;

        private JSONArray toJson(String jsonString){
            try {
                return new JSONArray(jsonString);
            } catch (JSONException e) {
                Log.e(TAG,e.getMessage(),e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected JSONArray doInBackground(String... urls) {
            StringBuilder builder = new StringBuilder();
            HttpClient httpclient = new DefaultHttpClient();
            try {
                HttpResponse httpResponse = httpclient.execute(new HttpGet(urls[0]));
                StatusLine statusLine = httpResponse.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = httpResponse.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    String result = builder.toString();
                    return toJson(result);
                }else{
                    errorMsg = "网络错误";
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                e.printStackTrace();
                errorMsg = e.getMessage();
            }
            cancel(true);
            return null;
        }

        @Override
        protected  void onPostExecute(JSONArray PDFFile) {
            success(PDFFile);
        }

        @Override
        protected void onCancelled(){
            error(errorMsg);
        }
    }
}


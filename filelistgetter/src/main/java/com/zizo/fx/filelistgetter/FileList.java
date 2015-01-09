package com.zizo.fx.filelistgetter;


import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
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
    FileListAsyncTask flat;

    public FileList() {
        flat = new FileListAsyncTask();
    }

    public void get(String url){
        flat.execute(url);
    }

    public void success(JSONArray ja){

    }

    public void error(int statueCode){

    }

    public class FileListAsyncTask extends HttpAsyncTask<JSONArray,Integer>{
        private String getData(String url) {
            StringBuilder builder = new StringBuilder();
            HttpClient httpclient = new DefaultHttpClient();
            try {
                HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
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
                    onSuccessEvent(toJson(result));
                    return result;
                }else {
                    onErrorEvent(statusCode);
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                e.printStackTrace();
                onErrorEvent(500);
            }
            return null;
        }
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
        protected String doInBackground(String... urls) {
            return getData(urls[0]);
        }

        @Override
        public void onSuccessEvent(JSONArray sp) {
            success(sp);
        }

        @Override
        public void onErrorEvent(Integer ep) {
            error(ep);
        }
    }
}


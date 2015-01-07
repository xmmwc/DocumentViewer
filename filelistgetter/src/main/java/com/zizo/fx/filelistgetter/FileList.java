package com.zizo.fx.filelistgetter;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 由 XMM 于 2015-01-07.创建
 */
public class FileList {
    final private String Tag = "FileList";
    OnSuccessEvent mOnSuccessEvent;
    OnErrorEvent mOnErrorEvent;

    public FileList() {

    }

    public FileList get(String url){
        new HttpAsyncTask().execute(url);
        return this;
    }

    public FileList success(OnSuccessEvent onSuccessEvent){
        mOnSuccessEvent = onSuccessEvent;
        return this;
    }

    public FileList error(OnErrorEvent onErrorEvent) {
        mOnErrorEvent = onErrorEvent;
        return this;
    }

    private JSONObject toJson(String jsonString){
        try {
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            Log.e(Tag,e.getMessage(),e);
            e.printStackTrace();
        }
        return null;
    }

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
            }else{
                if (mOnErrorEvent !=null)
                    mOnErrorEvent.error(statusCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return getData(urls[0]);
        }
        @Override
        protected void onPostExecute(String result) {
            if (mOnSuccessEvent!=null)
                mOnSuccessEvent.success(toJson(result));
        }
    }

    public interface OnSuccessEvent{
        public void success(JSONObject data);
    }

    public interface OnErrorEvent {
        public void error(int statusCode);
    }
}

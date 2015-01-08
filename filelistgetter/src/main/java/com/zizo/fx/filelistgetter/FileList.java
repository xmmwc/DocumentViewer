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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 由 XMM 于 2015-01-07.创建
 */
public class FileList {
    private final String TAG = "FileList";
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

    private JSONArray toJson(String jsonString){
        try {
            return new JSONArray(jsonString);
        } catch (JSONException e) {
            Log.e(TAG,e.getMessage(),e);
            e.printStackTrace();
        }
        return null;
    }



    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
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
                    if (mOnSuccessEvent != null)
                        mOnSuccessEvent.success(toJson(result));
                    return result;
                }else{
                    if (mOnErrorEvent !=null)
                        mOnErrorEvent.error(statusCode);
                }
            } catch (IOException e) {
                Log.e(TAG,e.getMessage(),e);
                e.printStackTrace();
                if (mOnErrorEvent !=null)
                    mOnErrorEvent.error(500);
            }
            return null;
        }


        @Override
        protected String doInBackground(String... urls) {
            return getData(urls[0]);
        }
        @Override
        protected void onPostExecute(String result) {

        }
    }

    public interface OnSuccessEvent{
        public void success(JSONArray data);
    }

    public interface OnErrorEvent {
        public void error(int statusCode);
    }
}

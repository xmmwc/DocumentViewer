package com.zizo.fx.filelistgetter;

import android.os.AsyncTask;
import org.json.JSONArray;

public abstract class HttpAsyncTask<SuccessParams,ErrorParams> extends AsyncTask<String, Void, String> {
    private final String TAG = "HttpAsyncTask";

    @Override
    protected abstract String doInBackground(String... urls);

    public abstract void onSuccessEvent(SuccessParams sp);

    public abstract void onErrorEvent (ErrorParams ep);
}

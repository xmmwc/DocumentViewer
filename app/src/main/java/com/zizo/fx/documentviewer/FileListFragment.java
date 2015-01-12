package com.zizo.fx.documentviewer;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 由 XMM 于 2015-01-12.创建
 */
public class FileListFragment extends Fragment {
    private String Tag = "FileListFragment";
    private String Host = "http://192.168.1.64:1213";
    private String APIPath = Host + "/API/FileInfo";
    private String DownloadPath = Host + "/API/File";

    private PullToRefreshListView mListView;
    public List<FileItem> mFileItems;
    private long mLastUpdateTime;

    private List<FileItem> getFileItems(JSONArray data) {
        List<FileItem> fileItems = new ArrayList<>();
        try {
            for (int i = 0; i < data.length(); i++) {
                JSONObject jo = data.getJSONObject(i);
                String name = jo.getString("Name");
                int fileType = jo.getInt("FileType");
                String filePath = jo.getString("FilePath");
                int fileSize = jo.getInt("FileSize");
                fileItems.add(new FileItem(name, fileType, filePath, fileSize));
            }
        } catch (JSONException e) {
            Log.e(Tag, e.getMessage(), e);
        }
        return fileItems;
    }

    private void openFolder(FileItem fileItem) {
        FileListFragment nextFrag = new FileListFragment();

        Bundle bundle = new Bundle();
        bundle.putString("nextPath", "/" + fileItem.FilePath);
        bundle.putString("nextTitle", fileItem.Name);
        nextFrag.setArguments(bundle);

        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, nextFrag, fileItem.Name)
                .addToBackStack(null)
                .commit();
    }

    private void openPdf(FileItem fileItem) {
        Intent intent = new Intent(getActivity(), PdfViewActivity.class);
        intent.putExtra(PdfViewActivity.mPdfPath, DownloadPath + "/" + fileItem.FilePath);
        startActivity(intent);
    }

    private void doClick(FileItem fileItem) {
        switch (fileItem.FileType) {
            case 1:
                openFolder(fileItem);
                break;
            case 2:
                openPdf(fileItem);
                break;
            default:
                break;
        }
    }

    private void setListView(final List<FileItem> listItems) {
        FileListAdapter fileListAdapter = new FileListAdapter(getActivity(), listItems);
        mListView.setAdapter(fileListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileItem selectedItem = (FileItem)parent.getItemAtPosition(position);//listItems.get(position - 1);
                doClick(selectedItem);
            }
        });
    }

    private void setMenuTitle(){
        Bundle bundle = this.getArguments();
        String title;
        ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        try {
            title = bundle.getString("nextTitle");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(title);
        } catch (Exception e) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setTitle(R.string.title_activity_file_list);
        }
    }

    private void getFileList(boolean forceRefresh) {
        Bundle bundle = this.getArguments();
        String dirPath;
        try {
            dirPath = bundle.getString("nextPath");
        } catch (Exception e) {
            Log.i(Tag, "没有下级文件夹地址");
            dirPath = "";
        }
        if (mFileItems == null || forceRefresh) {
            new FileListAsyncTask().execute(APIPath + dirPath);
        } else {
            setListView(mFileItems);
        }
    }

    private String culLeftTime(long leftTime) {
        if (leftTime <= 1000)
            return "刚刚";
        if (leftTime <= 60000)
            return (int) leftTime / 1000 + "秒前";
        if (leftTime <= 3600000)
            return (int) leftTime / 60000 + "分钟前";

        return "很久以前";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setRetainInstance(true);
        View rootView = inflater.inflate(R.layout.fragment_file_list, container, false);
        mListView = (PullToRefreshListView) rootView.findViewById(R.id.file_list);
        mListView.setOnPullEventListener(new PullToRefreshBase.OnPullEventListener<ListView>() {
            @Override
            public void onPullEvent(PullToRefreshBase<ListView> refreshView, PullToRefreshBase.State state, PullToRefreshBase.Mode direction) {
                if (state.equals(PullToRefreshBase.State.PULL_TO_REFRESH)) {
                    refreshView.getLoadingLayoutProxy().setPullLabel(getString(R.string.pull_to_refresh));
                    refreshView.getLoadingLayoutProxy().setReleaseLabel(getString(R.string.release_to_refresh));
                    refreshView.getLoadingLayoutProxy().setRefreshingLabel(getString(R.string.loading));

                    long now  = System.currentTimeMillis();
                    long leftTime = now - mLastUpdateTime;

                    String label = culLeftTime(leftTime);
                    // Update the LastUpdatedLabel
                    refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(
                            getString(R.string.updated) + " : " + label);
                }
            }
        });
        mListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                getFileList(true);
            }
        });
        setMenuTitle();
        getFileList(false);

        return rootView;
    }

    private class FileListAdapter extends ArrayAdapter<FileItem> {
        private final Activity mContext;
        private List<FileItem> mAdapterFileItems;

        public FileListAdapter(Activity context, List<FileItem> fileItems) {
            super(context, R.layout.file_list_item, fileItems);
            mContext = context;
            mAdapterFileItems = fileItems;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            View itemView = inflater.inflate(R.layout.file_list_item, null, true);
            ImageView img = (ImageView) itemView.findViewById(R.id.item_img);
            TextView title = (TextView) itemView.findViewById(R.id.item_title);

            FileItem item = mAdapterFileItems.get(position);

            img.setImageResource(getImageId(item.FileType));
            title.setText(item.Name);

            return itemView;
        }

        private int getImageId(int fileType) {
            switch (fileType) {
                case 1:
                    return R.drawable.ic_folder;
                case 2:
                    return R.drawable.ic_pdf;
                default:
                    return R.drawable.ic_blank;
            }
        }
    }

    private class FileListTaskResult {
        private JSONArray data;
        private Exception error;
    }

    private class FileListAsyncTask extends AsyncTask<String, Void, FileListTaskResult> {

        private JSONArray toJson(String jsonString) {
            try {
                return new JSONArray(jsonString);
            } catch (JSONException e) {
                Log.e(Tag, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected FileListTaskResult doInBackground(String... urls) {
            FileListTaskResult result = new FileListTaskResult();
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
                    result.data = toJson(builder.toString());
                } else {
                    throw new HttpResponseException(statusCode, "网络错误:" + statusCode);
                }
            } catch (Exception e) {
                Log.e(Tag, e.getMessage(), e);
                result.error = e;
                cancel(true);
            }
            return result;
        }

        @Override
        protected void onPostExecute(FileListTaskResult result) {
            mFileItems = getFileItems(result.data);
            setListView(mFileItems);
            mListView.onRefreshComplete();
            mLastUpdateTime = System.currentTimeMillis();
        }

        @Override
        protected void onCancelled(FileListTaskResult result) {
            if (result.error != null && result.error instanceof HttpResponseException)
                Toast.makeText(getActivity(), result.error.getMessage(), Toast.LENGTH_SHORT).show();
            if (result.error != null && result.error instanceof HttpHostConnectException)
                Toast.makeText(getActivity(), "请确定是否连接网络", Toast.LENGTH_SHORT).show();
        }
    }

    private class FileItem {
        public String Name;
        public String FilePath;
        public int FileType;
        public int FileSize;

        public FileItem(String name, int fileType, String filePath, int fileSize) {
            Name = name;
            FilePath = filePath;
            FileType = fileType;
            FileSize = fileSize;
        }
    }
}
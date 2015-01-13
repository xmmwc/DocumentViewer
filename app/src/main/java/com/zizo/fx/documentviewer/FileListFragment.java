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
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * 列表堆栈窗体
 * 由 XMM 于 2015-01-12.创建
 */
public class FileListFragment extends Fragment {
    private String Tag = "FileListFragment";
    //服务器地址
    private String Host = "http://192.168.1.64:1213";
    //列表请求地址
    private String APIPath = Host + "/API/FileInfo";
    //文件流地址
    private String DownloadPath = Host + "/API/File";

    //下拉更新列表控件实例
    private PullToRefreshListView mListView;
    //列表数据
    public List<FileItem> mFileItems;
    //最后更新时间
    private long mLastUpdateTime;

    //转换Json数据
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

    //打开下级目录-用新地址创建新的Fragment
    private void openFolder(FileItem fileItem) {
        FileListFragment nextFrag = new FileListFragment();

        Bundle bundle = new Bundle();
        //附加下级目录地址
        bundle.putString("nextPath", "/" + fileItem.FilePath);
        //附加下级目录标题名称
        bundle.putString("nextTitle", fileItem.Name);
        nextFrag.setArguments(bundle);

        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, nextFrag, fileItem.Name)
                .addToBackStack(null)
                .commit();
    }

    //打开pdf文件
    private void openPdf(FileItem fileItem) {
        Intent intent = new Intent(getActivity(), PdfViewActivity.class);
        intent.putExtra(PdfViewActivity.mPdfPath, DownloadPath + "/" + fileItem.FilePath);
        startActivity(intent);
    }

    //执行列表点击操作
    private void doClick(FileItem fileItem) {
        switch (fileItem.FileType) {
            //如果是目录
            case 1:
                openFolder(fileItem);
                break;
            //如果是pdf
            case 2:
                openPdf(fileItem);
                break;
            default:
                break;
        }
    }

    //绑定列表数据
    private void setListView(List<FileItem> listItems) {
        FileListAdapter fileListAdapter = new FileListAdapter(getActivity(), listItems);
        mListView.setAdapter(fileListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //PullToRefreshListView的position是从1开始
                //所以从parent获取列表集合
                //listItems.get(position - 1);
                FileItem selectedItem = (FileItem)parent.getItemAtPosition(position);
                doClick(selectedItem);
            }
        });
    }

    //设置ActionBar的标题显示
    private void setMenuTitle(){
        Bundle bundle = this.getArguments();
        String title;
        //获得Activity的ActionBar
        ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();
        try {
            title = bundle.getString("nextTitle");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(title);
        } catch (Exception e) {
            //如果没有获取到标题则为根目录
            //禁用返回按钮
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setTitle(R.string.title_activity_file_list);
        }
    }

    //获取列表数据
    //forceRefresh为true时强制刷新
    private void getFileList(boolean forceRefresh) {
        Bundle bundle = this.getArguments();
        String dirPath;
        try {
            dirPath = bundle.getString("nextPath");
        } catch (Exception e) {
            Log.i(Tag, "没有下级文件夹地址");
            dirPath = "";
        }
        //如果列表为空或强制刷新是开启AsyncTask获取列表数据
        if (mFileItems == null || forceRefresh) {
            new FileListAsyncTask().execute(APIPath + dirPath);
        } else {
            setListView(mFileItems);
        }
    }

    //计算距离上次更新的时间
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
        //下拉更新准备事件
        mListView.setOnPullEventListener(new PullToRefreshBase.OnPullEventListener<ListView>() {
            @Override
            public void onPullEvent(PullToRefreshBase<ListView> refreshView, PullToRefreshBase.State state, PullToRefreshBase.Mode direction) {
                if (state.equals(PullToRefreshBase.State.PULL_TO_REFRESH)) {
                    //设置下拉更新提示
                    refreshView.getLoadingLayoutProxy().setPullLabel(getString(R.string.pull_to_refresh));
                    //设置释放更新提示
                    refreshView.getLoadingLayoutProxy().setReleaseLabel(getString(R.string.release_to_refresh));
                    //设置正在更新提示
                    refreshView.getLoadingLayoutProxy().setRefreshingLabel(getString(R.string.loading));

                    //获得现在时间
                    long now  = System.currentTimeMillis();
                    long leftTime = now - mLastUpdateTime;

                    String label = culLeftTime(leftTime);
                    // 设置上次更新时间提示
                    refreshView.getLoadingLayoutProxy().setLastUpdatedLabel(
                            getString(R.string.updated) + " : " + label);
                }
            }
        });
        //下拉更新事件
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

    //列表数据适配器-用于绑定列表数据
    private class FileListAdapter extends ArrayAdapter<FileItem> {
        private Activity mContext;
        private List<FileItem> mAdapterFileItems;

        public FileListAdapter(Activity context, List<FileItem> fileItems) {
            super(context, R.layout.file_list_item, fileItems);
            mContext = context;
            mAdapterFileItems = fileItems;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = mContext.getLayoutInflater();
            //使用自定义视图R.layout.file_list_item创建列表选项
            View itemView = inflater.inflate(R.layout.file_list_item, null, true);
            //列表图标
            ImageView img = (ImageView) itemView.findViewById(R.id.item_img);
            //列表标题
            TextView title = (TextView) itemView.findViewById(R.id.item_title);

            //要创建的列表数据
            FileItem item = mAdapterFileItems.get(position);

            //设置图标
            img.setImageResource(getImageId(item.FileType));
            //设置标题
            title.setText(item.Name);

            return itemView;
        }

        //获得图标id
        private int getImageId(int fileType) {
            switch (fileType) {
                //如果为目录
                case 1:
                    return R.drawable.ic_folder;
                //如果为pdf文件
                case 2:
                    return R.drawable.ic_pdf;
                default:
                    return R.drawable.ic_blank;
            }
        }
    }

    //异步任务返回结果类
    private class FileListTaskResult {
        private JSONArray data;
        private Exception error;
    }

    //获取列表数据异步任务
    private class FileListAsyncTask extends AsyncTask<String, Void, FileListTaskResult> {

        //序列化Json字符串
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
            //http客户端
            HttpClient httpclient = new DefaultHttpClient();
            try {
                //http请求
                HttpResponse httpResponse = httpclient.execute(new HttpGet(urls[0]));
                StatusLine statusLine = httpResponse.getStatusLine();
                //服务器返回代码
                int statusCode = statusLine.getStatusCode();
                if (statusCode == HttpURLConnection.HTTP_OK) {
                    HttpEntity entity = httpResponse.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    //绑定结果
                    result.data = toJson(builder.toString());
                } else {
                    throw new HttpResponseException(statusCode, "网络错误:" + statusCode);
                }
            } catch (Exception e) {
                Log.e(Tag, e.getMessage(), e);
                //绑定异常
                result.error = e;
                //取消任务
                cancel(true);
            }
            return result;
        }

        //任务执行后事件
        @Override
        protected void onPostExecute(FileListTaskResult result) {
            mFileItems = getFileItems(result.data);
            setListView(mFileItems);
            //告知列表视图刷新完成
            mListView.onRefreshComplete();
            //保存刷新时间
            mLastUpdateTime = System.currentTimeMillis();
        }

        //任务取消事件
        @Override
        protected void onCancelled(FileListTaskResult result) {
            //如果是服务器错误
            if (result.error != null && result.error instanceof HttpResponseException)
                Toast.makeText(getActivity(), result.error.getMessage(), Toast.LENGTH_SHORT).show();
            //如果是连接错误
            if (result.error != null && result.error instanceof HttpHostConnectException)
                Toast.makeText(getActivity(), "请确定是否连接网络", Toast.LENGTH_SHORT).show();
            //告知列表视图刷新完成
            mListView.onRefreshComplete();
            //保存刷新时间
            mLastUpdateTime = System.currentTimeMillis();
        }
    }

    //列表数据实体
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
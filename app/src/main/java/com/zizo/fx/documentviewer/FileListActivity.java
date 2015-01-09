package com.zizo.fx.documentviewer;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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


public class FileListActivity extends ActionBarActivity {

    final static String Tag = "FileList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new FileListFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file_list, menu);
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


    public static class FileListFragment extends Fragment {
        private String Host = "http://192.168.1.64:1213";
        private String APIPath = Host + "/API/FileInfo";
        private String DownloadPath = Host + "/API/File";

        private View rootView;
        private ListView listView;
        private FileListAdapter fileListAdapter;
        public List<FileItem> mFileItems;

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
            nextFrag.setArguments(bundle);

            this.getFragmentManager().beginTransaction()
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
            fileListAdapter = new FileListAdapter(getActivity(), listItems);
            listView.setAdapter(fileListAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    FileItem selectedItem = listItems.get(position);
                    doClick(selectedItem);
                }
            });
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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setRetainInstance(true);
            rootView = inflater.inflate(R.layout.fragment_file_list, container, false);
            listView = (ListView) rootView.findViewById(R.id.file_list);

            getFileList(false);

            return rootView;
        }

        private class FileListAdapter extends ArrayAdapter<FileItem> {
            private final Activity mContext;
            private final List<FileItem> mFileItems;

            public FileListAdapter(Activity context, List<FileItem> fileItems) {
                super(context, R.layout.file_list_item, fileItems);
                mContext = context;
                mFileItems = fileItems;
            }

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                LayoutInflater inflater = mContext.getLayoutInflater();
                View itemView = inflater.inflate(R.layout.file_list_item, null, true);
                ImageView img = (ImageView) itemView.findViewById(R.id.item_img);
                TextView title = (TextView) itemView.findViewById(R.id.item_title);

                FileItem item = mFileItems.get(position);

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

}

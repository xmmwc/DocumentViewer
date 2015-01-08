package com.zizo.fx.documentviewer;

import android.app.Activity;
import android.app.LauncherActivity;
import android.os.Handler;
import android.os.Looper;
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

import com.zizo.fx.filelistgetter.FileList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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
        static final String APIPath = "http://192.168.1.64:1213/api/fileinfo";

        private View rootView;
        private ListView listView;
        private FileListAdapter fileListAdapter;
        private List<FileItem> mfileItems;

        private List<FileItem> getFileItems(JSONArray data){
            List<FileItem> fileItems = new ArrayList<>();
            try {
                for (int i=0;i<data.length();i++){
                    JSONObject jo = data.getJSONObject(i);
                    String name = jo.getString("Name");
                    int fileType = jo.getInt("FileType");
                    String filePath = jo.getString("FilePath");
                    int fileSize = jo.getInt("FileSize");
                    fileItems.add(new FileItem(name,fileType,filePath,fileSize));
                }
            } catch (JSONException e) {
                Log.e(Tag,e.getMessage(),e);
                e.printStackTrace();
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
            //todo:
        }

        private void doClick(FileItem fileItem){
            switch (fileItem.FileType){
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

        private void getFileList() {
            Bundle bundle = this.getArguments();
            String dirPath = "";
            try {
                dirPath = bundle.getString("nextPath");
            } catch (Exception e) {
                Log.i(Tag, "没有下级文件夹地址");
                dirPath = "";
            }
            new FileList().get(APIPath + dirPath)
                    .success(new FileList.OnSuccessEvent() {
                        @Override
                        public void success(JSONArray data) {
                            mfileItems = getFileItems(data);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    fileListAdapter = new FileListAdapter(getActivity(), mfileItems);
                                    listView.setAdapter(fileListAdapter);
                                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                        @Override
                                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            FileItem selectedItem = mfileItems.get(position);
                                            doClick(selectedItem);
                                        }
                                    });
                                }
                            });
                        }
                    })
                    .error(new FileList.OnErrorEvent() {
                        @Override
                        public void error(int statusCode) {
                            Toast.makeText(rootView.getContext(), "网络连接失败", Toast.LENGTH_SHORT);
                        }
                    });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setRetainInstance(true);
            rootView = inflater.inflate(R.layout.fragment_file_list, container, false);
            listView = (ListView) rootView.findViewById(R.id.file_list);

            getFileList();


//            Button btnOpen = (Button)rootView.findViewById(R.id.open_btn);
//            btnOpen.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Intent intent = new Intent(getActivity(),PdfViewActivity.class);
//                    String path = Environment.getExternalStorageDirectory().getPath();
//                    intent.putExtra(PdfViewActivity.mPdfPath,path + "/mm.pdf");
//                    startActivity(intent);
//                }
//            });

            return rootView;
        }
    }

    public static class FileListAdapter extends ArrayAdapter<FileItem>{
        private final Activity mContext;
        private final List<FileItem> mFileItems;

        public FileListAdapter(Activity context,List<FileItem> fileItems) {
            super(context, R.layout.file_list_item, fileItems);
            mContext = context;
            mFileItems = fileItems;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent){
            LayoutInflater inflater = mContext.getLayoutInflater();
            View itemView = inflater.inflate(R.layout.file_list_item,null,true);
            ImageView img = (ImageView) itemView.findViewById(R.id.item_img);
            TextView title = (TextView) itemView.findViewById(R.id.item_title);

            FileItem item = mFileItems.get(position);

            img.setImageResource(getImageId(item.FileType));
            title.setText(item.Name);

            return itemView;
        }

        private int getImageId(int fileType){
            switch (fileType){
                case 1:
                    return R.drawable.ic_folder;
                case 2:
                    return R.drawable.ic_pdf;
                default:
                    return R.drawable.ic_blank;
            }
        }

        static private List<String> getTitles(List<FileItem> fileItems){
            List<String> titleList = new ArrayList<>();

            for (int i = 0; i < fileItems.size(); i++) {
                titleList.add(fileItems.get(i).Name);
            }
            return titleList;
        }
    }

    public static class FileItem{
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

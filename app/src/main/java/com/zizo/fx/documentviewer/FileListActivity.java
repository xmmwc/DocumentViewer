package com.zizo.fx.documentviewer;

import android.app.ListFragment;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.zizo.fx.filelistgetter.FileList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class FileListActivity extends ActionBarActivity {

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

        private List<FileItem> getFileItems(JSONObject data){
            List<FileItem> fileItems = new ArrayList<>();
            try {
                JSONArray ja = data.getJSONArray("FileInfo");
                for (int i=0;i<ja.length();i++){
                    JSONObject jo = ja.getJSONObject(i);
                    String name = jo.getString("Name");
                    String fileType = jo.getString("FileType");
                    String filePath = jo.getString("FilePath");
                    int fileSize = jo.getInt("FileSize");
                    fileItems.add(new FileItem(name,fileType,filePath,fileSize));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return fileItems;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setRetainInstance(true);
            final View rootView = inflater.inflate(R.layout.fragment_file_list, container, false);
            final ListView lv = (ListView) rootView.findViewById(R.id.file_list);

            new FileList().get("http://192.168.1.64/api/fileinfo")
            .success(new FileList.OnSuccessEvent() {
                @Override
                public void success(JSONObject data) {
                    final List<FileItem> fileItems = getFileItems(data);
                    new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            ArrayAdapter<FileItem> arrayAdapter =
                                    new ArrayAdapter<FileItem>(rootView.getContext(),android.R.layout.simple_list_item_1,fileItems);
                            lv.setAdapter(arrayAdapter);

                        }
                    });
                }
            });

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

    public static class FileItem{
        public String Name;
        public String FilePath;
        public String FileType;
        public int FileSize;

        public FileItem(String name, String fileType, String filePath, int fileSize) {
            Name = name;
            FilePath = filePath;
            FileType = fileType;
            FileSize = fileSize;
        }
    }
}

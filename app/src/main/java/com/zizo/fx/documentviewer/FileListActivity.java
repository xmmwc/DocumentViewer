package com.zizo.fx.documentviewer;

import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.zizo.fx.filelistgetter.FileList;
import org.json.JSONObject;


public class FileListActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setRetainInstance(true);
            View rootView = inflater.inflate(R.layout.fragment_file_list, container, false);
            ListView lv = (ListView) rootView.findViewById(R.id.file_list);

            new FileList().get("http://192.168.1.64/api/fileinfo")
            .success(new FileList.OnSuccessEvent() {
                @Override
                public void success(JSONObject data) {
                    new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            //lv.setAdapter();
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
}

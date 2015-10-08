package com.mydownloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_UPDATE="ACTION_UPDATE";
    private static final String ACTION_PAUSE_CLICKED="ACTION_PAUSE_CLICKED";
    private static final String ACTION_PAUSE_DONE="ACTION_PAUSE_DONE";
    private static final String ACTION_FINISH="ACTION_FINISH";
    private static final String PATH="path";
    private static final String ISWORKING="isWorking";
    private static final String TEST_PATH="http://7xn38b.com1.z0.glb.clouddn.com/music/music1.mp3";


    private List<Map<String,Object>> dloadList;
    private ListView dloadListView;
    private SimpleAdapter dloadAdapter;

    private List<DloadItem> dloadItems;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dloadListView=(ListView)findViewById(R.id.main_lv_items);
        dloadItems=new ArrayList<>();
        dloadList=new ArrayList<>();
        dloadAdapter=new SimpleAdapter(this,dloadList,R.layout.dload_list,
                new String[]{DloadItem.FILENAME,DloadItem.PROGRESS,DloadItem.CONDITION},
                new int[]{R.id.list_tv_name,R.id.list_tv_prog,R.id.list_btn_pause});
        dloadListView.setAdapter(dloadAdapter);

        IntentFilter filter=new IntentFilter(ACTION_UPDATE);
        filter.addAction(ACTION_PAUSE_DONE);
        filter.addAction(ACTION_FINISH);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,filter);

    }

    BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            DloadItem item;
            if(action.equals(ACTION_UPDATE)){
                for(int i=0;i<dloadItems.size();i++){
                    item=dloadItems.get(i);
                    item.setProgress(intent.getIntExtra(item.fPath, item.progress));
                }
            }else if(action.equals(ACTION_FINISH)){
                String tempPath=intent.getStringExtra(PATH);
                for(int i=0;i<dloadItems.size();i++){
                    item=dloadItems.get(i);
                    if(item.fPath.equals(tempPath)){
                        dloadItems.remove(i);
                        dloadList.remove(i);
                        break;
                    }
                }
            }else if(action.equals(ACTION_PAUSE_DONE)){
                String tempPath=intent.getStringExtra(PATH);
                System.out.println("Receive:"+tempPath);
                for(int i=0;i<dloadItems.size();i++){
                    item=dloadItems.get(i);
                    if(item.fPath.equals(tempPath)){
                        item.setWorking(intent.getBooleanExtra(ISWORKING,false));
                        System.out.println("Pause done: " + item.isWorking);
                        dloadAdapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
            update();
        }
    };



    public void on_download_clicked(View source){
        EditText et = (EditText) findViewById(R.id.main_et_path);
        String path =et.getText().toString();

        Intent intent=new Intent(MainActivity.this,DownloadIntentService.class);
        intent.putExtra(PATH, path);
        startService(intent);

        DloadItem item=new DloadItem(path);
        dloadItems.add(item);
        dloadList.add(item.filedMap);
        dloadAdapter.notifyDataSetChanged();

    }

    public void on_test_clicked(View source){
        EditText et = (EditText) findViewById(R.id.main_et_path);
        et.setText(TEST_PATH);
    }

    public void on_pause_clicked(View source){
        View parentRow=(View)source.getParent();
        int position=dloadListView.getPositionForView(parentRow);

        Intent intent=new Intent(ACTION_PAUSE_CLICKED);
        intent.putExtra(PATH,dloadItems.get(position).fPath);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void update(){
        for(int i=0;i<dloadList.size();i++){
            dloadList.set(i,dloadItems.get(i).filedMap);
        }
        dloadAdapter.notifyDataSetChanged();
    }

    private class DloadItem{
        public static final String FILENAME="FILENAME";
        public static final String PROGRESS="PROGRESS";
        public static final String CONDITION="CONDITION";

        public String fPath;
        public String fName;
        public int progress;
        public boolean isWorking;
        public Map<String,Object> filedMap;

        public DloadItem(String fPath){
            filedMap=new HashMap<>();
            setFilePath(fPath);
            setProgress(0);
            setWorking(true);
        }

        public String getCondition(){
            return isWorking?"暂停":"继续";
        }

        public void setFilePath(String fPath){
            this.fPath=fPath;
            this.fName=fPath.substring(fPath.lastIndexOf("/") + 1, fPath.length());
            filedMap.put(FILENAME,fName);
        }

        public void setProgress(int progress){
            this.progress=progress;
            filedMap.put(PROGRESS,progress+"%");
        }

        public void setWorking(boolean isWorking){
            this.isWorking=isWorking;
            filedMap.put(CONDITION,getCondition());
        }

        public DloadItem(String fPath,int progress){}


    }
}

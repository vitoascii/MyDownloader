package com.mydownloader;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DownloadIntentService extends IntentService {

    private static final String ACTION_UPDATE="ACTION_UPDATE";
    private static final String ACTION_PAUSE_CLICKED="ACTION_PAUSE_CLICKED";
    private static final String ACTION_PAUSE_DONE="ACTION_PAUSE_DONE";
    private static final String ACTION_FINISH="ACTION_FINISH";
    private static final String PATH="path";
    private static final String ISWORKING="isWorking";

    private String targetDir;
    private static int ntfId=0;

    private ExecutorService dlThreadPool;
    private NotificationManager manager=null;
    private List<Downloader> downloaderList;
    private List<NtfItem> notifList;




    public DownloadIntentService() {
        super("DownloadIntentService");

        downloaderList=new ArrayList<>();
        notifList=new ArrayList<>();
        dlThreadPool= Executors.newCachedThreadPool();

        IntentFilter filter=new IntentFilter(ACTION_PAUSE_CLICKED);
        filter.addAction(ACTION_FINISH);
        filter.addAction(ACTION_PAUSE_DONE);
        filter.addAction(ACTION_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,filter);

        try{
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                targetDir = Environment.getExternalStorageDirectory()
                    .getCanonicalPath() + "/MyDownloader";
                File targetDirFile = new File(targetDir);
                targetDirFile.mkdir();
            }
        }catch(IOException e){
            e.printStackTrace();
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run(){
                Intent intent=new Intent(ACTION_UPDATE);
                Downloader tmpDlder;
                for(int i=0;i<downloaderList.size();i++){
                    tmpDlder=downloaderList.get(i);
                    intent.putExtra(tmpDlder.path,tmpDlder.getProgress());
                    if(tmpDlder.getProgress()==100){
                        Intent intent1=new Intent(ACTION_FINISH);
                        intent1.putExtra(PATH,tmpDlder.path);
                        LocalBroadcastManager.getInstance(DownloadIntentService.this)
                                .sendBroadcast(intent1);
                    }
                }
                LocalBroadcastManager.getInstance(DownloadIntentService.this).sendBroadcast(intent);

            }
        },0,800);
    }

    BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(manager==null)
                manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            String action=intent.getAction();
            if(action.equals(ACTION_PAUSE_CLICKED)){
                System.out.println("RECEIVE!");
                String pausePath=intent.getStringExtra(PATH);
                Downloader tmpDlder;
                for(int i=0;i<downloaderList.size();i++){
                    tmpDlder=downloaderList.get(i);
                    if(tmpDlder.path.equals(pausePath)){
                        Intent intent1=new Intent(ACTION_PAUSE_DONE);
                        intent1.putExtra(ISWORKING,tmpDlder.changeCondition());
                        intent1.putExtra(PATH,pausePath);
                        LocalBroadcastManager.getInstance(DownloadIntentService.this)
                                .sendBroadcast(intent1);
                        break;
                    }
                }
            }else if(action.equals(ACTION_UPDATE)){
                NtfItem ntfItem;
                for(int i=0;i<notifList.size();i++){
                    ntfItem=notifList.get(i);
                    ntfItem.update(intent.getIntExtra(ntfItem.path,ntfItem.progress));
                    manager.notify(ntfItem.id, ntfItem.notify);
                }
            }else if(action.equals(ACTION_FINISH)){
                NtfItem ntfItem;
                String tmpPath=intent.getStringExtra(PATH);
                for(int i=0;i<notifList.size();i++){
                    ntfItem=notifList.get(i);
                    if(ntfItem.path.equals(tmpPath)){
                        notifList.remove(i);
                        manager.cancel(ntfItem.id);
                        break;
                    }
                }
            }else if(action.equals(ACTION_PAUSE_DONE)){
                NtfItem ntfItem;
                String tmpPath=intent.getStringExtra(PATH);
                for(int i=0;i<notifList.size();i++){
                    ntfItem=notifList.get(i);
                    if(ntfItem.path.equals(tmpPath)){
                        ntfItem.setCondition(intent.getBooleanExtra(ISWORKING,true));
                        manager.notify(ntfItem.id,ntfItem.notify);
                    }
                }
            }
        }
    };



    @Override
    protected void onHandleIntent(Intent intent) {
        String path=intent.getStringExtra(PATH);
        String fName=path.substring(path.lastIndexOf("/") + 1, path.length());

        Downloader downloader=new Downloader(path,targetDir+"/"+fName);
        downloader.download(dlThreadPool);
        downloaderList.add(downloader);

        NtfItem ntfItem=new NtfItem(path,ntfId);
        notifList.add(ntfItem);

    }

    @Override
    public void onDestroy(){
        dlThreadPool.shutdown();
        super.onDestroy();
    }

    private class NtfItem {
        public int id;
        public String path;
        public RemoteViews view=null;
        public Notification notify=new Notification();
        public Intent intent =null;
        public PendingIntent pIntent=null;
        public int progress;

        public NtfItem(String path,int id){
            view=new RemoteViews(getPackageName(),R.layout.notify);
            this.path=path;
            this.id=id;
            ntfId++;
            this.progress=0;
            String fName=path.substring(path.lastIndexOf("/") + 1, path.length());

            view.setTextViewText(R.id.ntf_tv_name, fName);
            view.setImageViewResource(R.id.image, R.drawable.ic_launcher);
            intent=new Intent(DownloadIntentService.this,MyService.class);
            intent.setAction(ACTION_PAUSE_CLICKED);
            intent.putExtra(PATH, path);
            pIntent=PendingIntent.getService(DownloadIntentService.this,0,intent,0);
            update(0);
            setCondition(true);
            notify.icon=R.drawable.ic_launcher;
            notify.contentView=view;
            notify.contentIntent=pIntent;
        }

        public void update(int progress){
            this.progress=progress;
            view.setProgressBar(R.id.ntf_pb_prog, 100, progress, false);
            notify.contentView=view;
            notify.contentIntent=pIntent;
        }

        public void setCondition(boolean isWorking){
            view.setTextViewText(R.id.ntf_tv_pause, isWorking ? "下载中" : "暂停中");
            notify.contentView=view;
            notify.contentIntent=pIntent;
        }
    }


}

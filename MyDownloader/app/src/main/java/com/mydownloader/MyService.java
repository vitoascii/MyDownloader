package com.mydownloader;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class MyService extends Service {
    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        System.out.println("Start!");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        return START_STICKY;
    }
}

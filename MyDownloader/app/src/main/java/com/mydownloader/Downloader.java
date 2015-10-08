package com.mydownloader;


import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;

public class Downloader {
    public String path;
    public String targetPath;
    public int threadNum=5;
    public MyThread[] myThreads;
    public int size;
    public boolean isWorking;


    public Downloader(String path,String targetPath){
        this.path=path;
        this.isWorking=false;
        this.targetPath=targetPath;


    }

    public void download(ExecutorService dlThreadPool){
        int partSize;
        try{
            myThreads=new MyThread[threadNum];
            URL fileUrl=new URL(path);
            HttpURLConnection fileCnn=(HttpURLConnection)fileUrl.openConnection();
            size=fileCnn.getContentLength();
            fileCnn.disconnect();

            partSize=size/threadNum+1;
            RandomAccessFile file=new RandomAccessFile(targetPath,"rw");
            file.setLength(size);
            file.close();

        for(int i=0;i<threadNum;i++){
            int startPos=i*partSize;
            RandomAccessFile partFile=new RandomAccessFile(targetPath,"rw");
            partFile.seek(startPos);
            myThreads[i]=new MyThread(startPos,partSize,partFile);
            dlThreadPool.submit(myThreads[i]);
            }
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public int getProgress(){
        if(size!=0) {
            int loadedSize = 0;
            for (int i = 0; i < myThreads.length; i++) {
                if(myThreads[i]!=null){
                    loadedSize += myThreads[i].length;
                }
            }
            return 100 * loadedSize / size;
        }else
            return 0;

    }

    public boolean changeCondition(){
        isWorking=!isWorking;
        for(int i=0;i<myThreads.length;i++){
            if(myThreads[i]!=null){
                myThreads[i].setCondition(isWorking);
            }
        }
        return isWorking;
    }


    private class MyThread implements Runnable{
        private final String control="";
        public int startPos;
        public boolean isWorking;
        public int length;
        public RandomAccessFile dlFile;
        public int partSize;

        public MyThread(int startPos,int partSize,RandomAccessFile dlFile){
            this.startPos=startPos;
            this.dlFile=dlFile;
            this.length=0;
            this.partSize=partSize;
        }

        public void run(){
            try{
                isWorking=true;
                URL url=new URL(path);
                HttpURLConnection conn=(HttpURLConnection)url.openConnection();
                InputStream in=conn.getInputStream();
                in.skip(this.startPos);
                byte[] buffer=new byte[10*1024];
                int hasRead=0;
                while((length<partSize)&&((hasRead=in.read(buffer))!=-1)){
                    if(!isWorking){
                        synchronized (control){
                            try{
                                control.wait();
                            }catch(InterruptedException  e){
                                e.printStackTrace();
                            }
                        }
                    }
                    dlFile.write(buffer,0,hasRead);
                    length+=hasRead;
                }
                dlFile.close();
                in.close();
            }
            catch(Exception e){
                e.printStackTrace();
            }

        }

        public void setCondition(boolean isWorking){
            if(this.isWorking!=isWorking){
                this.isWorking=isWorking;
                if(isWorking){
                    synchronized (control){
                        control.notifyAll();
                    }
                }
            }

        }




    }
}

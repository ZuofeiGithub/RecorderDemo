package com.testopensourceapplication.recorderdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ZHao on 2016/12/12.
 */
public class RecordUtils {
    public static final String FILE_DATA = "Data";

    /**
     * 获得内外置SD卡的/Android/data/你的应用的包名/files/ 目录
     */
     public static List getAppPaths(Context context){
         List pathsList =new ArrayList();
         //存储的管理器
         StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
         //获取sdcard 的路径 外置和内置
         String[] paths =null;
         try{
             paths = (String[]) sm.getClass().getMethod("getVolumePaths").invoke(sm);
         }catch (Exception e){
             e.printStackTrace();
         }
         for(int i=0;i<paths.length;i++){
             pathsList.add(paths[i]);
         }
        return pathsList;
    }
    /**
     * 当内存卡容量少于1000M时，自动删除视频列表里面的第一个文件
     */
    public static  void spaceNotEnoughDeleteTempFile(Context context,String filepath){
        try {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                File file = new File(filepath);
                if(!file.exists()){
                    boolean mkdirs =file.mkdirs();
                }
                StatFs  statFs = new StatFs(filepath);
                // 获取block的SIZE
                long blocSize = statFs.getBlockSize();
                // 获取总的BLOCK数量
                long totalBlocks = statFs.getBlockCount();
                // 可使用的Block的数量
                long availaBlock = statFs.getAvailableBlocks();
                // 获取当前可用内存容量，单位：MB
                long sd = availaBlock * blocSize / 1024 / 1024;

                while (sd<1000){
                    String filepath1 =filepath + "/temporary";
                    File  file1 = new File(filepath1);
                    if(!file1.exists()){
                        file1.mkdirs();
                    }
                    File[] files =file1.listFiles();
                    if(files.length>0){
                        String dele =files[0] +"";
                        File file2 =new File(dele);
                        file2.delete();
                    }

                }
            }else if(Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED)){
                Toast.makeText(context, "请插入内存卡", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * 获取当前时间 format:yyyy-MM-dd HH:mm:ss
     */
    public static String getCurrentTime(String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        return df.format(new Date());
    }


    /**
     * 格式化时间
     */
    public static String format(int i) {
        String s = i + "";
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }

    /**
     * 存储数据
     */
    public  static  void putData(Context context,boolean data){
        SharedPreferences spf =context.getSharedPreferences(FILE_DATA,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =spf.edit();
        editor.putBoolean("isRecording",data);
        editor.commit();
    }
}

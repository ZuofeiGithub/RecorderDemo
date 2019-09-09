package com.testopensourceapplication.recorderdemo;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

/**
 * Created by ZHao on 2016/12/13.
 */
public class BackgroundRecoder extends Service implements SurfaceHolder.Callback {
    private NotificationManager manager;
    private Notification.Builder notifiBuilder;
    private Notification notifi;

    private String innerSDcard;// 储存内部SD卡路径
    private String rootpath;

    private static final int NOTIFICATION_DI = 1234;
    private static final int LAUNCH_RECORD = 0x001;

    private WindowManager mWindowManager;
    private View mRecorderView;
    private SurfaceView mSurfaceView;
    private ImageView move;
    ImageButton btn_start;
    ImageButton btn_stop;
    ImageButton btn_close;
    private boolean isRecording = false;

    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;

    private WindowManager.LayoutParams mLayoutParams;

    private float x;
    private float y;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == LAUNCH_RECORD) {
                startRecord();
            }
            super.handleMessage(msg);
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        showNotifaction();
        innerSDcard = Environment.getExternalStorageDirectory().getPath();
        rootpath = MainActivity.rootpath;

        File file =new File(innerSDcard + File.separator + "infisight" + File.separator +
              "Recorders"+File.separator + "temporary");
         if(!file.exists()){

             file.mkdirs();
         }
        startForeground(NOTIFICATION_DI,notifi);

        mWindowManager  = (WindowManager) getSystemService(WINDOW_SERVICE);
        mRecorderView = LayoutInflater.from(this).inflate(
                R.layout.recorder_layout, null);

        mSurfaceView = (SurfaceView) mRecorderView.findViewById(R.id.sv_recorder);
        move = (ImageView) mRecorderView.findViewById(R.id.move);
        btn_start = (ImageButton) mRecorderView.findViewById(R.id.btn_start);
        btn_stop  = (ImageButton) mRecorderView.findViewById(R.id.btn_stop);
        btn_close = (ImageButton) mRecorderView.findViewById(R.id.btn_close);

        mSurfaceView.getHolder().addCallback(this);
        paramsButtonSetOnClick();

        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.type =WindowManager.LayoutParams.TYPE_PHONE;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;

        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;// 设置悬浮窗口长宽数据
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowManager.addView(mRecorderView, mLayoutParams);


        mRecorderView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                x = event.getRawX();
                y =event.getRawY();
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        Animation animation1 = AnimationUtils.loadAnimation(
                                BackgroundRecoder.this, R.anim.params_anim1);
                        // close.startAnimation(animation1);
                        move.startAnimation(animation1);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        updateViewPosition(event);
                        break;
                    case MotionEvent.ACTION_UP:
                        updateViewPosition(event);
                        // 动画
                        Animation animation = AnimationUtils.loadAnimation(
                                BackgroundRecoder.this, R.anim.params_anim);
                        // close.startAnimation(animation);
                        move.startAnimation(animation);
                        break;
                }
                return false;
            }
        });


    }

    private  void updateViewPosition(MotionEvent event){
        mLayoutParams.x = (int) event.getRawX() - move.getWidth() / 2;
        mLayoutParams.y = (int) event.getRawY() - move.getHeight() / 2;
        mWindowManager.updateViewLayout(mRecorderView, mLayoutParams);

    }



    /**
     * 显示一个通知
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showNotifaction(){
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pendingIntent =PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),0);
        notifiBuilder = new Notification.Builder(this);
        notifi = notifiBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("行车记录仪")
                .setContentText("")
                .setAutoCancel(true).build();
    }

   private  void paramsButtonSetOnClick(){
       btn_start.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               startRecord();
           }
       });

       btn_stop.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               stopRecord();
           }
       });

       btn_close.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               onDestroy();
           }
       });

   }

    private void startRecord(){
        RecordUtils.spaceNotEnoughDeleteTempFile(BackgroundRecoder.this, rootpath);
        btn_start.setVisibility(View.INVISIBLE);
        btn_stop.setVisibility(View.VISIBLE);
        isRecording = true;
        RecordUtils.putData(BackgroundRecoder.this,isRecording);
        String nowTime = RecordUtils.getCurrentTime("yyyyMMddHHmmss");

        mMediaRecorder =new MediaRecorder();
        openCamera();
        if(mCamera!=null){
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);
            CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setOutputFormat(camcorderProfile.fileFormat);
            mMediaRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
            mMediaRecorder.setVideoEncoder(camcorderProfile.videoCodec);

            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

            mMediaRecorder.setOutputFile(innerSDcard + File.separator + "infisight" + File.separator + "Recorders"
                    + File.separator + "temporary" + File.separator + nowTime + ".mp4");

            try{
                // 准备录制（prepare为耗时操作，会造成主界面的ANR，如果可以，尽量放在子线程中）（导致开始录像时画面卡顿的原因）
                mMediaRecorder.prepare();
            }catch (Exception e){

            }

            mMediaRecorder.start();
           // MainActivity.progressDialog.dismiss();
        }


    }

    /**
     * 打开摄像头
     */
    public void openCamera(){
        int cameraCount = android.hardware.Camera.getNumberOfCameras();
        android.hardware.Camera.CameraInfo cameraInfo =new android.hardware.Camera.CameraInfo();
        int cameraUse =0;
        for(int camIdx=0;camIdx<cameraCount;camIdx++){
            Camera.getCameraInfo(camIdx,cameraInfo);
            try{
                cameraUse++;
                mCamera = Camera.open(camIdx);
                break;
            }catch (Exception e){
            }
        }
        if(cameraUse == 0){
            Toast.makeText(this, "无法搜索到摄像设备", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 停止录像
     */
    private void stopRecord(){
        btn_start.setVisibility(View.VISIBLE);
        btn_stop.setVisibility(View.VISIBLE);
        if(mMediaRecorder!=null){
            isRecording = false;
            RecordUtils.putData(BackgroundRecoder.this,isRecording);

            //停止录制
            mMediaRecorder.stop();
            mMediaRecorder.reset();

            // 释放资源
            mMediaRecorder.release();
            mMediaRecorder =null;

        }
    }


    @Override
    public void onDestroy() {
        try{
            if(mMediaRecorder!=null){
                //停止录制
                mMediaRecorder.stop();
                mMediaRecorder.reset();
               //释放资源
                mMediaRecorder.release();
                mMediaRecorder = null;

            }
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        btn_start.setVisibility(View.VISIBLE);
        btn_stop.setVisibility(View.INVISIBLE);
        if(mCamera!=null){
            mCamera.release();
            mCamera = null;

        }
        if (mWindowManager != null && mRecorderView.getVisibility() == View.VISIBLE) {
            mWindowManager.removeView(mRecorderView);
        }
        stopForeground(true);

        super.onDestroy();

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
        // 如果之前状态为录制中，继续开启录像
        if (true) {
            handler.sendEmptyMessageDelayed(LAUNCH_RECORD, 500);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceView = null;
        mSurfaceHolder = null;
        mMediaRecorder = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

package com.testopensourceapplication.recorderdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener ,SurfaceHolder.Callback{

    public static CustomProgressDialog progressDialog;

    SurfaceView surfaceView;

    TextView timer;
    ImageView recording;

    LinearLayout ll_hud_record_viewc;
    TextView start;
    TextView save;
    TextView  photo;
    TextView stop;


    private String innerSDcard;// 储存内部SD卡路径
    public static String rootpath;
    private String extSDcard;// 储存外部SD卡的路径（需要root权限）
    private MediaRecorder mediaRecorder;// 录制视频的类
    private SensorManager sensorManager;// 检测震动相关对象（加速度传感器）


    private final int MENU_HIDE_MSG = 0x001;
    private final int TIMER_GOING_MSG = 0x002;
    private final int RECORDER_RESTART_MSG = 0x003;
    private final int LAUNCH_RECORD = 0x004;
    private int textColor;
    private boolean isRecording = false;
    private String nowTime;
    private int second;
    private int minute;
    private int hour;
    private Camera mCamera;
    private SurfaceHolder surfaceHolder;
    private Camera.Size previewSize;
    private boolean safeToTakePicture = false;
    private SoundPool soundPool;
    private int cameraSound;// 音效的ID


    /**
     * 定时器设置，实现计时
     */
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TIMER_GOING_MSG) {

                handler.sendEmptyMessageDelayed(TIMER_GOING_MSG, 1000);
                second++;
                if (second >= 60) {
                    minute++;
                    second = second % 60;
                }
                if (minute >= 60) {
                    hour++;
                    minute = minute % 60;
                }
                timer.setText(RecordUtils.format(hour) + ":"
                        + RecordUtils.format(minute) + ":"
                        + RecordUtils.format(second));
            }
            if (msg.what == RECORDER_RESTART_MSG) {
                prepareRecorder();
            }
            if (msg.what == MENU_HIDE_MSG) {
                start.setVisibility(View.GONE);
                stop.setVisibility(View.GONE);
                save.setVisibility(View.GONE);
                photo.setVisibility(View.GONE);

            }
            if (msg.what == LAUNCH_RECORD) {
                RecordUtils.spaceNotEnoughDeleteTempFile(MainActivity.this, rootpath);
                if (mediaRecorder == null) {
                    prepareRecorder();
                } else {
                    Toast.makeText(MainActivity.this, "视频录制中", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            super.handleMessage(msg);
        }

    };

    //拍照的回调
    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            String path = innerSDcard + "/infisight/Recorders/photo/";
            File photoFile  = new File(path);
            if(!photoFile.exists()){
                photoFile.mkdirs();
            }
            if (isRecording == false) {
                mCamera.stopPreview();
            }
            File pictureFile  =new File(path,System.currentTimeMillis()+".jpg");
            try{
                FileOutputStream fos =new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            }catch (Exception e){
                Log.d("takePhoto", "File not found: " + e.getMessage());
            }
            safeToTakePicture = true;
            if (isRecording == false) {
                mCamera.startPreview();
            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initActivity();
    }

    private void initView(){
        // SurfaceView 用于承载画面
        surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        timer = (TextView) findViewById(R.id.timer);
        recording = (ImageView) findViewById(R.id.recording);
        ll_hud_record_viewc = (LinearLayout) findViewById(R.id.ll_hud_record_view);
        start = (TextView) findViewById(R.id.start);
        save = (TextView) findViewById(R.id.save);
        photo = (TextView) findViewById(R.id.photo);
        stop = (TextView) findViewById(R.id.stop);


        SurfaceHolder holder = surfaceView.getHolder();// 取得holder
        holder.addCallback(this); // holder加入回调接口

        // 用于开始录像时播放的声音   音效的数量，类型，质量
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        //加载声音资源
        cameraSound = soundPool.load(MainActivity.this, R.raw.camera, 0);

        start.setOnClickListener(this);
        save.setOnClickListener(this);
        photo.setOnClickListener(this);
        stop.setOnClickListener(this);

        textColor = getTextColor(stop);

        disenableView(stop);
    }

    public void requestAllPower() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }


    private void  initActivity(){
        innerSDcard = Environment.getExternalStorageDirectory().getPath();

        requestAllPower();

        List extSDCardPath = RecordUtils.getAppPaths(MainActivity.this);


        for(int i=0;i<extSDCardPath.size();i++){
            if(extSDCardPath.get(i).equals(innerSDcard)){
                rootpath = innerSDcard;
                break;
            }
            extSDcard = extSDCardPath.get(i) + "";

        }

        //如果没有外置SD卡用内置
        File[] externalFilesDirs =new File[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            externalFilesDirs = this.getExternalFilesDirs(null);
        }
        for(int i=0;i<externalFilesDirs.length;i++){
            if (externalFilesDirs[i] != null) {
                rootpath = externalFilesDirs[0] + "";
            }
        }
        // 获取传感器管理器
        sensorManager = (SensorManager) this.getSystemService(
                Context.SENSOR_SERVICE);
    }


    @Override
    protected void onResume() {
        if (sensorManager != null) {// 注册监听器
            sensorManager.registerListener(sensorEventListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
            // 第一个参数是Listener，第二个参数是所得传感器类型，第三个参数值获取传感器信息的频率
        }

        super.onResume();
    }

    // 震动传感器监听
    private SensorEventListener sensorEventListener = new SensorEventListener() {

       // 传感器数据变动事件
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] valuer = event.values;
            float x =valuer[0];// x轴方向的重力加速度，向右为正
            float y =valuer[1];// y轴方向的重力加速度，向前为正
            float z =valuer[2];// z轴方向的重力加速度，向上为正
            // LogTools.i("Sensor", "x轴方向的重力加速度" + x + "；y轴方向的重力加速度" + y +
            // "；z轴方向的重力加速度" + z);
            int medumValue =20;
          if (Math.abs(x) > medumValue || Math.abs(y) > medumValue
                    || Math.abs(z) > medumValue) {
                RecordUtils.spaceNotEnoughDeleteTempFile(MainActivity.this, rootpath);
                if (mediaRecorder == null) {
                    prepareRecorder();
                } else {
                    Toast.makeText(MainActivity.this, "视频录制中", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    /**
     * 设置MediaRecorder（初始化）
     */

    public void prepareRecorder(){
        enableView(stop);
        disenableView(start);
        recording.setVisibility(View.VISIBLE);
        isRecording = true;
        // 每次准备录像的时候生成nowTime作为文件名（可以确保每次录制的文件名不一样）
        nowTime = RecordUtils.getCurrentTime("yyyyMMddHHmmss");
        second = 0;
        minute = 0;
        hour = 0;
        timer.setVisibility(View.VISIBLE);
        handler.sendEmptyMessage(TIMER_GOING_MSG);

        File file = new File(rootpath + "/temporary/");
        if (!file.exists()) {
            file.mkdirs();
        }
        mediaRecorder = new MediaRecorder();// 创建mediarecorder对象

        CamcorderProfile camcorderProfile = CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH);

        unlockCamera();

        // 设置声源
        // mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置录制视频源为Camera(相机)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        // 可以设置视频录制完以后输出的角度
        mediaRecorder.setOrientationHint(0);
        //设置输出格式
        mediaRecorder.setOutputFormat(camcorderProfile.fileFormat);
        //设置视频帧率
        mediaRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);

        mediaRecorder.setVideoSize(camcorderProfile.videoFrameWidth,
                camcorderProfile.videoFrameHeight);

        mediaRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);

        //设置视频编码
        mediaRecorder.setVideoEncoder(camcorderProfile.videoCodec);
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        // 设置视频文件输出的路径

        // mediaRecorder.setOutputFile("/sdcard/driveVideo.mp4");
        //设置输出路径
        mediaRecorder.setOutputFile(rootpath + "/temporary/"
                + nowTime + ".mp4");

        // 设置录制文件最长时间(10分钟)
        mediaRecorder.setMaxDuration(60000 * 10);
        try {
            // 准备录制（prepare为耗时操作，会造成主界面的ANR，如果可以，尽量放在子线程中）（导致开始录像时画面卡顿的原因）
            mediaRecorder.prepare();

            //开始录制
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 激活控件
     */
    private void enableView(TextView v) {
        v.setClickable(true);
        v.setTextColor(textColor);
    }

    /**
     * 注销控件
     */
    private void disenableView(TextView v) {
        v.setClickable(false);
        v.setTextColor(Color.parseColor("#474747"));

    }

    /**
     * 获取文字颜色
     *
     * @param v
     * @return
     */
    private int getTextColor(TextView v) {
        ColorStateList textColors = v.getTextColors();
        int defaultColor = textColors.getDefaultColor();
        return defaultColor;
    }

    /**
     * 开始摄像前的准备工作
     */
    private void unlockCamera() {
        if (mCamera != null) {
            mCamera.unlock();
            mediaRecorder.setCamera(mCamera);
        } else {
        }

    }


    /*
       当 Surface第一次创建后会立即调用该函数。
    *     程序可以在该函数中做些和绘制界面相关的初始化工作，
    *     一般情况下都是在另外的线程来绘制界面，
    **/

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 将holder，这个holder为开始在oncreat里面取得的holder，将它赋给surfaceHolder
        Log.e("TAG","surfaceCreated1");
        surfaceHolder = holder;
        startCamera(holder);
    }

    /*当Surface的状态（大小和格式）
    发生变化的时候会调用该函数，
    在surfaceCreated调用后该函数至少会
    被调用一次。*/
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("TAG","surfaceCreated2");
        surfaceHolder = holder;
        if (surfaceHolder.getSurface() == null) {
            // 预览surface不存在
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {

        }
        if (mCamera != null) {

            Log.e("TAG","safeToTakePicture"+safeToTakePicture);
            //设置摄像头的初始参数
            Camera.Parameters parameters = mCamera.getParameters();
            previewSize = parameters.getPreviewSize();
            mCamera.startPreview();
            safeToTakePicture = true;
        }

    }

   /* 当Surface被摧毁前会调用该函数
     该函数被调用后就不能继续使用Surface了，
    一般在该函数中来清理使用的资源。*/
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("TAG","surfaceCreated3");
        if (mediaRecorder != null) {
            stopRecorder();
        }
        surfaceView = null;
        surfaceHolder = null;
        mediaRecorder = null;
        if (mCamera != null) {
            mCamera.autoFocus(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    @Override
    public void onClick(View v) {
        if(v == start){
            RecordUtils.spaceNotEnoughDeleteTempFile(MainActivity.this, rootpath);
            if (mediaRecorder == null && !isRecording) {
                prepareRecorder();
            } else {
                Toast.makeText(MainActivity.this, "视频录制中", Toast.LENGTH_SHORT)
                        .show();
            }

        }
        if(v == stop){
            if(mediaRecorder == null){
                Toast.makeText(MainActivity.this, "当前没有录制，无法停止", Toast.LENGTH_SHORT)
                        .show();
            }
            stopRecorder();
            isRecording = false;
            recording.setVisibility(View.GONE);
        }

        if(v == save){
            // 必须释放camera资源否则会导致camera被占用，无法被再次开启
            stopRecorder();
            isRecording = false;
            recording.setVisibility(View.GONE);
            Intent intent =new Intent(MainActivity.this,HomeActivity.class);
            startActivity(intent);

            Intent service =new Intent(MainActivity.this,BackgroundRecoder.class);
            startService(service);
            MainActivity.progressDialog.show();
        }

        if(v == photo){
            // ID,左声道，右声道，，循环次数，速率
            soundPool.play(cameraSound, 1f, 1f, 0, 0, 1f);
             try{
                 if (safeToTakePicture) {
                     mCamera.takePicture(null, null, mPictureCallback);
                     safeToTakePicture = false;
                     Toast.makeText(MainActivity.this, "拍照", Toast.LENGTH_SHORT)
                             .show();
                 }
             }catch (Exception e){
                 Toast.makeText(MainActivity.this, "拍照失败", Toast.LENGTH_SHORT)
                         .show();
                 e.printStackTrace();
             }


        }
    }

    private void startCamera(SurfaceHolder holder){
        openCamera();
        if(mCamera!=null){

            Camera.Parameters parameters =mCamera.getParameters();

            //判断屏幕是横屏或是竖屏
            if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                parameters.set("orientation", "portrait");//镜头角度转90度（默认摄像头是横拍）
                mCamera.setDisplayOrientation(90);
            }else{// 如果是横屏
                parameters.set("orientation", "landscape");
                mCamera.setDisplayOrientation(0);
            }
            int fittedWidth = 640;
            int fittedHeight = 480;
           // 指定preview的大小
            previewSize = mCamera.new Size(fittedWidth, fittedHeight);

            //指定拍照图片的大小
            parameters.setPreviewSize(fittedWidth, fittedHeight);

            // 将Camera.Parameters设定予Camera
            mCamera.setParameters(parameters);
            try {

                mCamera.setPreviewDisplay(holder);
            }catch (Exception e){
                e.printStackTrace();
            }

            // 打开预览画面
            mCamera.startPreview();
        }

    }

    /**
     * 打开摄像头
     */
    public  void openCamera(){
        int cameraCount =Camera.getNumberOfCameras();  //得到相机数
         Camera.CameraInfo   cameraInfo = new Camera.CameraInfo();
        int cameraUse =0;
        for(int camIdx=0;camIdx<cameraCount;camIdx++){
            Camera.getCameraInfo(camIdx,cameraInfo); //得到相机信息
            try {
                cameraUse++;
                mCamera = Camera.open(camIdx);
                break;
            }catch (Exception e){
            }
        }if(cameraUse == 0){
            Toast.makeText(MainActivity.this, "无法搜索到摄像设备", Toast.LENGTH_SHORT)
                    .show();
        }

    }
    /**
     * 停止录制并解锁camera
     */
    private void stopRecorder(){
        if(mediaRecorder != null){
            try{
                mediaRecorder.stop();
            }catch (Exception e){
                Toast.makeText(MainActivity.this, "您操作速度过快，小瑞正全力加载，请稍候",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            mediaRecorder.reset();
            // 释放资源
            mediaRecorder.release();
            mediaRecorder = null;
            // 释放相机，否则预览将会停止
        }
        enableView(start);
        disenableView(stop);
        isRecording = false;
        handler.removeMessages(TIMER_GOING_MSG);
        timer.setVisibility(View.GONE);
        if (mCamera != null) {
            mCamera.lock();
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {// 取消监听器
            sensorManager.unregisterListener(sensorEventListener);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        super.onDestroy();
    }
}

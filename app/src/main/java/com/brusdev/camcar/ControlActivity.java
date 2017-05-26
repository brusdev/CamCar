package com.brusdev.camcar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import x1.Studio.Core.IVideoDataCallBack;
import x1.Studio.Core.OnlineService;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ControlActivity extends AppCompatActivity implements SensorEventListener, SurfaceHolder.Callback,
        IVideoDataCallBack {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private TextView mContentView;
    private boolean mVisible;

    private OnlineService ons;
    public static boolean isInitLan = false;
    private Bitmap VideoBit;
    private boolean VideoInited = false;
    private Rect RectOfRegion;
    private RectF RectOfScale;
    private Matrix rotator;
    private int Width;
    private int Height;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private DevInfo devInfo;
    private String callID;


    private Thread thread;
    private Socket tcSocket;
    private byte[] tcBuffer;
    private Charset charset;
    private InputStream tcInputStream;
    private OutputStream tcOutputStream;

    private boolean stop;
    private boolean go_forward;
    private boolean go_backward;
    private boolean turn_left;
    private boolean turn_right;
    private Runnable stop_run;
    private Runnable go_forward_run;
    private Runnable go_backward_run;
    private Runnable turn_left_run;
    private Runnable turn_right_run;
    private ThreadPoolExecutor threadPoolExecutor;
    private BlockingDeque<Runnable> threadWorkQueue;


    private SensorManager mSensorManager;
    private Sensor mASensor;
    private Sensor mMSensor;
    private boolean mBackwardDown;
    private boolean mForwardDown;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_control);

        ons = OnlineService.getInstance();
        ons.setCallBackData(this);

        if (!isInitLan) {
            isInitLan = true;
            ons.initLan(); //��ʼ��������;

        }

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView_video);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        ons.regionAudioDataServer();
        ons.regionVideoDataServer();

        devInfo = new DevInfo();


        mVisible = true;
        mContentView = (TextView) findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.forward_fab).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.backward_fab).setOnTouchListener(mDelayHideTouchListener);


        /*
        findViewById(R.id.fab_go).setBackgroundTintList(
                ColorStateList.valueOf(Color.GREEN));

        findViewById(R.id.fab_stop).setBackgroundTintList(
                ColorStateList.valueOf(Color.RED));
                */

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mASensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        this.charset = Charset.forName("US-ASCII");
        this.tcBuffer = new byte[128];

        go_forward = false;
        go_backward = false;
        turn_left = false;
        turn_right = false;

        stop_run = new Runnable() {
            @Override
            public void run() {
                try {
                    sendCommand("0");
                } catch (Exception e) {
                    e.printStackTrace();
                    //runOnUiThread(new ToastRunnable(MainActivity.this, e.toString(), Toast.LENGTH_LONG));
                }
            }
        };
        go_forward_run = new Runnable() {
            @Override
            public void run() {
                try {
                    sendCommand("1");
                } catch (Exception e) {
                    e.printStackTrace();
                    //runOnUiThread(new ToastRunnable(MainActivity.this, e.toString(), Toast.LENGTH_LONG));
                }
            }
        };
        go_backward_run = new Runnable() {
            @Override
            public void run() {
                try {
                    sendCommand("2");
                } catch (Exception e) {
                    e.printStackTrace();
                    //runOnUiThread(new ToastRunnable(MainActivity.this, e.toString(), Toast.LENGTH_LONG));
                }
            }
        };
        turn_left_run = new Runnable() {
            @Override
            public void run() {
                try {
                    sendCommand("3");
                } catch (Exception e) {
                    e.printStackTrace();
                    //runOnUiThread(new ToastRunnable(MainActivity.this, e.toString(), Toast.LENGTH_LONG));
                }
            }
        };
        turn_right_run = new Runnable() {
            @Override
            public void run() {
                try {
                    sendCommand("4");
                } catch (Exception e) {
                    e.printStackTrace();
                    //runOnUiThread(new ToastRunnable(MainActivity.this, e.toString(), Toast.LENGTH_LONG));
                }
            }
        };
        this.threadWorkQueue = new LinkedBlockingDeque<>();
        this.threadPoolExecutor = new ThreadPoolExecutor(
                1,
                1,
                1,
                TimeUnit.SECONDS,
                this.threadWorkQueue);


        this.start();
    }

    private void start()
    {
        this.thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ControlActivity.this.run();
            }
        });

        this.thread.start();
    }

    private void run()
    {
        try {

            /*
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress_label.setText("connecting...");
                }
            });
            */

            this.tcSocket = null;

            while (this.tcSocket == null) {
                try
                {
                    this.tcSocket = new Socket();
                    this.tcSocket.setSoTimeout(3000);

                    this.tcSocket.connect(new InetSocketAddress(InetAddress.getByName("192.168.4.1"), 9003));
                }
                catch (Exception socketException) {
                    socketException.printStackTrace();

                    this.tcSocket = null;
                }
            }

            /*
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progress_view.setVisibility(View.GONE);
                }
            });
            */


            tcInputStream = this.tcSocket.getInputStream();
            tcOutputStream = this.tcSocket.getOutputStream();
        }
        catch (Exception e) {
            e.printStackTrace();
            //runOnUiThread(new ToastRunnable(MainActivity.this, e.toString(), Toast.LENGTH_LONG));
        }
    }

    private String sendCommand(String command) throws  Exception {

        int readBytes;

        synchronized (this.tcBuffer) {

            tcOutputStream.write(command.getBytes(charset));

            readBytes = tcInputStream.read(this.tcBuffer, 0, this.tcBuffer.length);

            while(readBytes < 2 || this.tcBuffer[readBytes - 2] != '\r' || this.tcBuffer[readBytes - 1] != '\n') {

                readBytes = readBytes + tcInputStream.read(this.tcBuffer, readBytes, this.tcBuffer.length - readBytes);
            }

            return new String(this.tcBuffer, 0, readBytes - 2, charset);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ons.setCallBackData(this);

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        mSensorManager.registerListener(this, mASensor,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMSensor,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        mSensorManager.unregisterListener(this);

        this.finish();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
            updateOrientationAngles();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
            updateOrientationAngles();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.

        if (mOrientationAngles[1] > 0.15) {
            if (!turn_left) {
                stop = false;
                turn_left = true;
                threadPoolExecutor.execute(turn_left_run);
            }
        } else if (mOrientationAngles[1] < -0.15) {
            if (!turn_right) {
                stop = false;
                turn_right = true;
                threadPoolExecutor.execute(turn_right_run);
            }
        } else {
            if (go_forward) {
                threadPoolExecutor.execute(go_forward_run);
            } else if (go_backward) {
                threadPoolExecutor.execute(go_backward_run);
            } else if (!stop){
                stop = true;
                turn_left = false;
                turn_right = false;
                threadPoolExecutor.execute(stop_run);
            }
        }

        mSensorHandler.removeCallbacks(mSensorRunnable);
        mSensorHandler.post(mSensorRunnable);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (view.getId() == R.id.backward_fab) {
                        mBackwardDown = true;
                        if (!go_backward) {
                            stop = false;
                            go_backward = true;
                            threadPoolExecutor.execute(go_backward_run);
                        }
                    }
                    if (view.getId() == R.id.forward_fab) {
                        mForwardDown = true;
                        if (!go_forward) {
                            stop = false;
                            go_forward = true;
                            threadPoolExecutor.execute(go_forward_run);
                        }
                    }

                    // touch down code
                    mSensorHandler.removeCallbacks(mSensorRunnable);
                    mSensorHandler.post(mSensorRunnable);
                    break;

                case MotionEvent.ACTION_MOVE:
                    // touch move code
                    break;

                case MotionEvent.ACTION_UP:
                    if (view.getId() == R.id.backward_fab) {
                        mBackwardDown = false;
                        go_backward = false;
                        if (!stop && !turn_left && !turn_right){
                            stop = true;
                            threadPoolExecutor.execute(stop_run);
                        }
                    }
                    if (view.getId() == R.id.forward_fab) {
                        mForwardDown = false;
                        go_forward = false;
                        if (!stop && !turn_left && !turn_right){
                            stop = true;
                            threadPoolExecutor.execute(stop_run);
                        }
                    }

                    // touch up code
                    mSensorHandler.removeCallbacks(mSensorRunnable);
                    mSensorHandler.post(mSensorRunnable);
                    break;
            }

            /*
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
            */

            return true;
        }
    };

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            System.out.println(ons.refreshLan());
        }
    };

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private final Handler mSensorHandler = new Handler();
    private final Runnable mSensorRunnable = new Runnable() {
        @Override
        public void run() {
            mContentView.setText(String.format("%.02f / %.02f / %.02f %s %s",
                    mOrientationAngles[0],
                    mOrientationAngles[1],
                    mOrientationAngles[2],
                    mForwardDown ? "fd" : "fu",
                    mBackwardDown ? "bd" : "bu"
            ));
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void initPlayer(int mFrameWidth, int mFrameHeight) {

        VideoInited = true;

        VideoBit = Bitmap.createBitmap(mFrameWidth, mFrameHeight,
                Bitmap.Config.RGB_565);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        this.Width = dm.widthPixels;
        this.Height = dm.heightPixels;

        // int Left = (this.Width - this.mFrameWidth) / 2;
        int Top = 0;
        int myHight = this.Height;
        int myWidth = (int) (mFrameWidth * (Double.valueOf(this.Height) / Double
                .valueOf(mFrameHeight)));
        int Left = (this.Width - myWidth) / 2;
        this.RectOfRegion = null;
        // this.RectOfScale = new RectF(Left, Top, Left + myWidth, myHight);
        this.RectOfScale = new RectF(0, 0, mSurfaceView.getHeight(), mSurfaceView.getWidth());
    }

    @Override
    public void OnCallbackFunForDataServer(String CallId, ByteBuffer Buf,
                                           int mFrameWidth, int mFrameHeight, int mEncode, int mTime) {

        if (!VideoInited) {
            initPlayer(mFrameWidth, mFrameHeight);

        }

        this.VideoBit.copyPixelsFromBuffer(Buf);
        try {
            Canvas canvas = mSurfaceHolder.lockCanvas(null);

            try {
                canvas.save();
                canvas.rotate(90, this.RectOfScale.width() / 2, this.RectOfScale.height() / 2);
                canvas.translate((this.RectOfScale.width() - this.RectOfScale.height()) / 2,
                        (this.RectOfScale.width() - this.RectOfScale.height()) / 2);
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(this.VideoBit, this.RectOfRegion,
                        this.RectOfScale, null);
                canvas.restore();
            } finally {
                this.mSurfaceHolder.unlockCanvasAndPost(canvas);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void OnCallbackFunForUnDecodeDataServer(String var1, byte[] var2, int var3, int var4, int var5, int var6, int var7) {

    }

    @Override
    public void OnCallbackFunForLanDate(String devid, String videoType, int hkid, int channal, int status, String audioType) {
        if (devid.equals("302")) {
            return;
        }

        devInfo.setDevid(devid);
        devInfo.setVideoType(videoType);
        devInfo.setHkid(hkid);
        devInfo.setChannal(channal);
        devInfo.setStats(status);
        devInfo.setAudioType(audioType);
        devInfo.setType(0);

        callID = ons.callLanVideo(devInfo.getDevid(),
                devInfo.getHkid(), devInfo.getVideoType(),
                devInfo.getChannal(), 0);

    }

    @Override
    public void OnCallbackFunForRegionMonServer(int iFlag) {

    }

    @Override
    public void OnCallbackFunForComData(int Type, int Result,
                                        int AttachValueBufSize, String AttachValueBuf) {

    }

    @Override
    public void OnCallbackFunForGetItem(byte[] byteArray, int result) {

    }

    @Override
    public void OnCallbackFunForIPRateData(String Ip) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }


    @Override
    protected void onDestroy() {
        if (callID != null) {
            ons.closeLanVideo(callID);
        }

        // ons.quitSysm();

        super.onDestroy();
    }
}

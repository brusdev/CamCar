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
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import io.github.controlwear.virtual.joystick.android.JoystickView;
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
    private static final int DEVICE_PITCH = 1;
    private static final int LEFT_JOYSTICK = 2;
    private static final int RIGHT_JOYSTICK = 3;
    private static final int MANUAL_JOYSTICK = 4;
    public static boolean isInitLan = false;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            /*
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
            */

            return true;
        }
    };
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
    private final Handler mCameraHandler = new Handler();
    private JoystickView mLeftJoystick;
    private JoystickView mRightJoystick;
    private ToggleButton mTurnModeButton;
    private ToggleButton mCameraButton;
    private ToggleButton mEngineButton;
    private TextView mContentView;
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
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private OnlineService ons;
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
    private final Runnable mCameraRunnable = new Runnable() {
        @Override
        public void run() {
            callID = ons.callLanVideo(devInfo.getDevid(),
                    devInfo.getHkid(), devInfo.getVideoType(),
                    devInfo.getChannal(), 0);
        }
    };
    private SensorManager mSensorManager;
    private Sensor mASensor;
    private Sensor mMSensor;
    private int mLeftJoystickAngle;
    private int mLeftJoystickStrength;
    private int mRightJoystickAngle;
    private int mRightJoystickStrength;
    private Thread mThread;
    private Runnable update_ui_run;
    private Handler mUIHandler;
    private Charset mCharset;
    private boolean mEnabled;
    private Object mEnableSync;
    private int mTurnMode;
    private int mLeftDriverDirection;
    private int mRightDriverDirection;
    private int mLeftDriverValue;
    private int mRightDriverValue;

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

        mLeftJoystickAngle = 0;
        mLeftJoystickStrength = 0;
        mRightJoystickAngle = 0;
        mRightJoystickStrength = 0;

        mTurnMode = 0;
        mEnabled = false;
        mLeftDriverValue = 0;
        mRightDriverValue = 0;


        mLeftJoystick = (JoystickView) findViewById(R.id.leftJoystickView);
        mLeftJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                mLeftJoystickAngle = angle;
                mLeftJoystickStrength = strength;
                ControlActivity.this.updateEngine();
            }
        });

        mRightJoystick = (JoystickView) findViewById(R.id.rightJoystickView);
        mRightJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                mRightJoystickAngle = angle;
                mRightJoystickStrength = strength;
                ControlActivity.this.updateEngine();
            }
        });

        mCameraButton = ((ToggleButton) findViewById(R.id.camera_tb));
        mCameraButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (callID != null) {
                            ons.closeLanVideo(callID);
                            devInfo.setHkid(0);
                            callID = null;
                        }

                        if (isChecked) {
                            System.out.println(ons.refreshLan());
                            /*
                            callID = ons.callLanVideo(devInfo.getDevid(),
                                    devInfo.getHkid(), devInfo.getVideoType(),
                                    devInfo.getChannal(), 0);
                                    */
                        }
                    }
                });

        mTurnModeButton = ((ToggleButton) findViewById(R.id.turn_controller_tb));
        mTurnModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTurnMode == DEVICE_PITCH) {
                    setTurnMode(LEFT_JOYSTICK);
                } else if (mTurnMode == LEFT_JOYSTICK) {
                    setTurnMode(RIGHT_JOYSTICK);
                } else if (mTurnMode == RIGHT_JOYSTICK) {
                    setTurnMode(MANUAL_JOYSTICK);
                } else {
                    setTurnMode(DEVICE_PITCH);
                }
            }
        });

        mEngineButton = ((ToggleButton) findViewById(R.id.engine_tb));
        mEngineButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                mEnabled = isChecked;
                synchronized(mEnableSync){
                    mEnableSync.notify();
                }
            }
        });

        setTurnMode(DEVICE_PITCH);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mASensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        this.mEnableSync = new Object();
        this.mCharset = Charset.forName("US-ASCII");
        mUIHandler = new Handler();
        update_ui_run = new Runnable() {
            @Override
            public void run() {
                mEngineButton.setChecked(mEnabled);
                mContentView.setText(String.format("%d/%d", mLeftDriverDirection == 0 ? mLeftDriverValue : -mLeftDriverValue, mRightDriverDirection == 0 ? mRightDriverValue : -mRightDriverValue));
            }
        };

        this.start();
    }

    private void start() {
        this.mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ControlActivity.this.run();
            }
        });

        this.mThread.start();
    }

    private void run() {
        try {
            int errorCount = 0;
            int targetPort = 309;
            DatagramSocket socket = new DatagramSocket();
            InetAddress targetAddress = InetAddress.getByName("192.168.10.9");

            socket.setSoTimeout(1000);

            byte[] message = new byte[1500];
            DatagramPacket targetPacket = new DatagramPacket(message, message.length);


            while (!this.isFinishing()) {
                //Wait enable event.
                synchronized(mEnableSync){
                    this.mEnableSync.wait();
                }

                while (!this.isFinishing() && this.mEnabled) {
                    String command = String.format("SET,%d,%d,%d,%d", mLeftDriverValue,
                            mRightDriverValue, mLeftDriverDirection, mRightDriverDirection);
                    byte[] commandByte = command.getBytes(mCharset);
                    DatagramPacket commandPacket = new DatagramPacket(commandByte,
                            commandByte.length, targetAddress, targetPort);
                    try {
                        //Send update.
                        socket.send(commandPacket);

                        //Read status.
                        socket.receive(targetPacket);

                        //Reset error count.
                        errorCount = 0;
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorCount++;

                        if (errorCount > 3) {
                            this.mEnabled = false;
                        }
                    }

                    //Check status changes.

                    //Update UI.
                    mUIHandler.removeCallbacks(update_ui_run);
                    mUIHandler.post(update_ui_run);

                    Thread.sleep(500);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //runOnUiThread(new ToastRunnable(MainActivity.this, e.toString(), Toast.LENGTH_LONG));
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

        synchronized(mEnableSync){
            mEnableSync.notify();
        }
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

        if (mTurnMode == DEVICE_PITCH) {
            updateEngine();
        }
    }

    private void setTurnMode(int mode) {
        if (mode == DEVICE_PITCH) {
            mTurnMode = DEVICE_PITCH;
            mTurnModeButton.setTextOn("PITCH");
            mLeftJoystick.setEnabled(false);
            mRightJoystick.setEnabled(true);
            mRightJoystick.setButtonDirection(1);
        } else if (mode == LEFT_JOYSTICK) {
            mTurnMode = LEFT_JOYSTICK;
            mTurnModeButton.setTextOn("LEFT");
            mLeftJoystick.setEnabled(true);
            mLeftJoystick.setButtonDirection(-1);
            mRightJoystick.setEnabled(true);
            mRightJoystick.setButtonDirection(1);
        } else if (mode == RIGHT_JOYSTICK) {
            mTurnMode = RIGHT_JOYSTICK;
            mTurnModeButton.setTextOn("RIGHT");
            mLeftJoystick.setEnabled(false);
            mRightJoystick.setEnabled(true);
            mRightJoystick.setButtonDirection(0);
        } else if (mode == MANUAL_JOYSTICK) {
            mTurnMode = MANUAL_JOYSTICK;
            mTurnModeButton.setTextOn("MANUAL");
            mLeftJoystick.setEnabled(true);
            mLeftJoystick.setButtonDirection(1);
            mRightJoystick.setEnabled(true);
            mRightJoystick.setButtonDirection(1);
        }

        mTurnModeButton.setChecked(true);
    }

    private void updateEngine() {

        int levels = 3;
        int turnLevel = 0;
        int speedLevel = 0;
        int maxValue = 1023;

        if (mTurnMode == MANUAL_JOYSTICK) {
            mLeftDriverValue = (mLeftJoystickStrength * levels / 100) * maxValue / levels;
            mRightDriverValue = (mRightJoystickStrength * levels / 100) * maxValue / levels;

            if (mLeftJoystickAngle >= 0 && mLeftJoystickAngle < 180) {
                mLeftDriverDirection = 1;
            } else {
                mLeftDriverDirection = 0;
            }

            if (mRightJoystickAngle >= 0 && mRightJoystickAngle < 180) {
                mRightDriverDirection = 1;
            } else {
                mRightDriverDirection = 0;
            }

        } else {
            speedLevel = mRightJoystickStrength * levels / 100;

            if (mRightJoystickAngle >= 180 && mRightJoystickAngle < 360) {
                speedLevel = -speedLevel;
            }

            if (mTurnMode == DEVICE_PITCH) {
                turnLevel = -(int) (mOrientationAngles[1] * (float) levels / 0.6);

                if (turnLevel > levels) {
                    turnLevel = levels;
                } else if (turnLevel < -levels) {
                    turnLevel = -levels;
                }

                if (speedLevel == 0 && turnLevel != 0) {
                    speedLevel = Math.abs(turnLevel);

                    if (turnLevel > 0) {
                        turnLevel = levels;
                    } else if (turnLevel < 0) {
                        turnLevel = -levels;
                    }
                }
            } else if (mTurnMode == LEFT_JOYSTICK) {
                turnLevel = mLeftJoystickStrength * levels / 100;

                if (mLeftJoystickAngle >= 90 && mLeftJoystickAngle < 270) {
                    turnLevel = -turnLevel;
                }

                if (speedLevel == 0 && turnLevel != 0) {
                    speedLevel = Math.abs(turnLevel);

                    if (turnLevel > 0) {
                        turnLevel = levels;
                    } else if (turnLevel < 0) {
                        turnLevel = -levels;
                    }
                }
            } else {
                if (mRightJoystickAngle >= 0 && mRightJoystickAngle < 90) {
                    turnLevel = (90 - mRightJoystickAngle) * (levels + 1) / 90;
                    if (turnLevel > levels) {
                        turnLevel = levels;
                    }
                } else if (mRightJoystickAngle >= 90 && mRightJoystickAngle < 180) {
                    turnLevel = (mRightJoystickAngle - 90) * (levels + 1) / 90;
                    if (turnLevel > levels) {
                        turnLevel = levels;
                    }
                    turnLevel = -turnLevel;
                } else if (mRightJoystickAngle >= 180 && mRightJoystickAngle < 270) {
                    turnLevel = (270 - mRightJoystickAngle) * (levels + 1) / 90;
                    if (turnLevel > levels) {
                        turnLevel = levels;
                    }
                    turnLevel = -turnLevel;
                } else if (mRightJoystickAngle >= 270 && mRightJoystickAngle < 360) {
                    turnLevel = (mRightJoystickAngle - 270) * (levels + 1) / 90;
                    if (turnLevel > levels) {
                        turnLevel = levels;
                    }
                }
            }

            if (speedLevel >= 0) {
                mLeftDriverDirection = 1;
                mRightDriverDirection = 1;
            } else {
                mLeftDriverDirection = 0;
                mRightDriverDirection = 0;
            }

            if (turnLevel == 0) {
                mLeftDriverValue = Math.abs(speedLevel) * maxValue / levels;
                mRightDriverValue = Math.abs(speedLevel) * maxValue / levels;
            } else if (turnLevel > 0) {
                mLeftDriverValue = Math.abs(speedLevel) * maxValue / levels;
                mRightDriverValue = Math.abs(speedLevel) * (levels - turnLevel) * maxValue / (levels * levels);
            } else {
                mLeftDriverValue = Math.abs(speedLevel) * (levels + turnLevel) * maxValue / (levels * levels);
                mRightDriverValue = Math.abs(speedLevel) * maxValue / levels;
            }
        }

        mUIHandler.removeCallbacks(update_ui_run);
        mUIHandler.post(update_ui_run);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

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

        mCameraHandler.removeCallbacks(mCameraRunnable);
        mCameraHandler.postDelayed(mCameraRunnable, 500);
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

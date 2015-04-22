/*
 * ControlDroneActivity
 * 
 * Created on: May 5, 2011
 * Author: Dmytro Baryskyy
 */

package com.parrot.freeflight.activities;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;

import com.parrot.freeflight.FreeFlightApplication;
import com.parrot.freeflight.R;
import com.parrot.freeflight.activities.base.ParrotActivity;
import com.parrot.freeflight.drone.DroneConfig;
import com.parrot.freeflight.drone.DroneConfig.EDroneVersion;
import com.parrot.freeflight.drone.NavData;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiver;
import com.parrot.freeflight.receivers.DroneBatteryChangedReceiverDelegate;
import com.parrot.freeflight.receivers.DroneCameraReadyActionReceiverDelegate;
import com.parrot.freeflight.receivers.DroneCameraReadyChangeReceiver;
import com.parrot.freeflight.receivers.DroneEmergencyChangeReceiver;
import com.parrot.freeflight.receivers.DroneEmergencyChangeReceiverDelegate;
import com.parrot.freeflight.receivers.DroneFlyingStateReceiver;
import com.parrot.freeflight.receivers.DroneFlyingStateReceiverDelegate;
import com.parrot.freeflight.receivers.DroneRecordReadyActionReceiverDelegate;
import com.parrot.freeflight.receivers.DroneRecordReadyChangeReceiver;
import com.parrot.freeflight.receivers.DroneVideoRecordStateReceiverDelegate;
import com.parrot.freeflight.receivers.DroneVideoRecordingStateReceiver;
import com.parrot.freeflight.receivers.WifiSignalStrengthChangedReceiver;
import com.parrot.freeflight.receivers.WifiSignalStrengthReceiverDelegate;
import com.parrot.freeflight.remotecontrollers.ButtonController;
import com.parrot.freeflight.remotecontrollers.ButtonDoubleClickController;
import com.parrot.freeflight.remotecontrollers.ButtonPressedController;
import com.parrot.freeflight.remotecontrollers.ButtonValueController;
import com.parrot.freeflight.remotecontrollers.ControlButtons;
import com.parrot.freeflight.remotecontrollers.ControlButtonsFactory;
import com.parrot.freeflight.sensors.DeviceOrientationChangeDelegate;
import com.parrot.freeflight.sensors.DeviceOrientationManager;
import com.parrot.freeflight.sensors.DeviceSensorManagerWrapper;
import com.parrot.freeflight.sensors.RemoteSensorManagerWrapper;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.settings.ApplicationSettings;
import com.parrot.freeflight.settings.ApplicationSettings.ControlMode;
import com.parrot.freeflight.settings.ApplicationSettings.EAppSettingProperty;
import com.parrot.freeflight.transcodeservice.TranscodingService;
import com.parrot.freeflight.ui.HudViewController;
import com.parrot.freeflight.ui.SettingsDialogDelegate;
import com.parrot.freeflight.ui.hud.AcceleroJoystick;
import com.parrot.freeflight.ui.hud.AnalogueJoystick;
import com.parrot.freeflight.ui.hud.JoystickBase;
import com.parrot.freeflight.ui.hud.JoystickFactory;
import com.parrot.freeflight.ui.hud.JoystickListener;
import com.parrot.freeflight.utils.NookUtils;
import com.parrot.freeflight.utils.SystemUtils;

@SuppressLint("NewApi")
public class ControlDroneActivity
        extends ParrotActivity
        implements DeviceOrientationChangeDelegate, WifiSignalStrengthReceiverDelegate, DroneVideoRecordStateReceiverDelegate, DroneEmergencyChangeReceiverDelegate,
        DroneBatteryChangedReceiverDelegate, DroneFlyingStateReceiverDelegate, DroneCameraReadyActionReceiverDelegate, DroneRecordReadyActionReceiverDelegate, SettingsDialogDelegate
{
    private static final int LOW_DISK_SPACE_BYTES_LEFT = 1048576 * 20; //20 mebabytes
    private static final int WARNING_MESSAGE_DISMISS_TIME = 5000; // 5 seconds

    private static final String TAG = "ControlDroneActivity";
    private static final float ACCELERO_TRESHOLD = (float) Math.PI / 180.0f * 2.0f;

    private static final int PITCH = 1;
    private static final int ROLL = 2;


    private DroneControlService droneControlService;
    private ApplicationSettings settings;
    private SettingsDialog settingsDialog;

    private JoystickListener rollPitchListener;
    private JoystickListener gazYawListener;

    private HudViewController view;

    private boolean useSoftwareRendering;
    // private boolean forceCombinedControlMode;

    private int screenRotationIndex;

    private WifiSignalStrengthChangedReceiver wifiSignalReceiver;
    private DroneVideoRecordingStateReceiver videoRecordingStateReceiver;
    private DroneEmergencyChangeReceiver droneEmergencyReceiver;
    private DroneBatteryChangedReceiver droneBatteryReceiver;
    private DroneFlyingStateReceiver droneFlyingStateReceiver;
    private DroneCameraReadyChangeReceiver droneCameraReadyChangedReceiver;
    private DroneRecordReadyChangeReceiver droneRecordReadyChangeReceiver;

    private SoundPool soundPool;
    private int batterySoundId;
    private int effectsStreamId;

    private boolean combinedYawEnabled;
    private boolean acceleroEnabled;
    private boolean magnetoEnabled;
    private boolean magnetoAvailable;
    private boolean controlLinkAvailable;

    private boolean pauseVideoWhenOnSettings;

    private DeviceOrientationManager deviceOrientationManager;

    private float pitchBase;
    private float rollBase;
    private boolean running;

    private boolean flying;
    private boolean recording;
    private boolean cameraReady;
    private boolean prevRecording;
    private boolean rightJoyPressed;
    private boolean leftJoyPressed;
    private boolean isGoogleTV;

    private ControllerInputs objControllerInputs = new ControllerInputs();


    private List<ButtonController> buttonControllers;

    private ServiceConnection mConnection = new ServiceConnection()
    {

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            droneControlService = ((DroneControlService.LocalBinder) service).getService();
            onDroneServiceConnected();
        }

        public void onServiceDisconnected(ComponentName name)
        {
            droneControlService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        settings = getSettings();

        this.isGoogleTV = SystemUtils.isGoogleTV(this);

        // TODO google TV requires specific sensor manager and device rotation
        if (this.isGoogleTV) {
            deviceOrientationManager = new DeviceOrientationManager(new RemoteSensorManagerWrapper(this), this);
        } else {
            screenRotationIndex = getWindow().getWindowManager().getDefaultDisplay().getRotation();
            deviceOrientationManager = new DeviceOrientationManager(new DeviceSensorManagerWrapper(this), this);
        }

        deviceOrientationManager.onCreate();

        bindService(new Intent(this, DroneControlService.class), mConnection, Context.BIND_AUTO_CREATE);

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            useSoftwareRendering = bundle.getBoolean("USE_SOFTWARE_RENDERING");
//            forceCombinedControlMode = bundle.getBoolean("FORCE_COMBINED_CONTROL_MODE");
        } else {
            useSoftwareRendering = false;
//            forceCombinedControlMode = false;
        }

        pauseVideoWhenOnSettings = getResources().getBoolean(R.bool.settings_pause_video_when_opened);

        combinedYawEnabled = true;
        acceleroEnabled = false;
        running = false;

        view = new HudViewController(this, useSoftwareRendering);

        wifiSignalReceiver = new WifiSignalStrengthChangedReceiver(this);
        videoRecordingStateReceiver = new DroneVideoRecordingStateReceiver(this);
        droneEmergencyReceiver = new DroneEmergencyChangeReceiver(this);
        droneBatteryReceiver = new DroneBatteryChangedReceiver(this);
        droneFlyingStateReceiver = new DroneFlyingStateReceiver(this);
        droneCameraReadyChangedReceiver = new DroneCameraReadyChangeReceiver(this);
        droneRecordReadyChangeReceiver = new DroneRecordReadyChangeReceiver(this);

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        batterySoundId = soundPool.load(this, R.raw.battery, 1);

        if (!deviceOrientationManager.isAcceleroAvailable()) {
            settings.setControlMode(ControlMode.NORMAL_MODE);
        }

        settings.setFirstLaunch(false);

    }


    @SuppressLint("NewApi")
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        // Process all historical movement samples in the batch
        final int historySize = event.getHistorySize();

        // Process the movements starting from the
        // earliest historical position in the batch
        for (int i = 0; i < historySize; i++) {
            // Process the event at historical position i
            objControllerInputs.processJoystickInput(event, i, droneControlService);
        }

        // Process the current movement sample in the batch (position -1)
        objControllerInputs.processJoystickInput(event, -1, droneControlService);
        return true;
//        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(objControllerInputs.processKeyDown(keyCode, event, droneControlService)){
            return true;
        }else{
            return false;
        }
    }


//
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event)
//    {
//        if (applyKeyEvent(event)) {
//            return true;
//        } else {
//            return super.onKeyDown(keyCode, event);
//        }
//    }
//
//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event)
//    {
//        if (applyKeyEvent(event)) {
//            return true;
//        } else {
//            return super.onKeyUp(keyCode, event);
//        }
//    }

    private boolean applyKeyEvent(KeyEvent theEvent)
    {
        boolean result = false;
        if (this.buttonControllers != null) {
            for (ButtonController controller : this.buttonControllers) {
                if (controller.onKeyEvent(theEvent)) {
                    result = true;
                }
            }
        }
        return result;
    }



    @Override
    protected void onDestroy()
    {

        this.deviceOrientationManager.destroy();

        soundPool.release();
        soundPool = null;

        unbindService(mConnection);

        super.onDestroy();
        Log.d(TAG, "ControlDroneActivity destroyed");
        System.gc();
    }

    private void registerReceivers()
    {
        // System wide receiver
        registerReceiver(wifiSignalReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));

        // Local receivers
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(videoRecordingStateReceiver, new IntentFilter(DroneControlService.VIDEO_RECORDING_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneEmergencyReceiver, new IntentFilter(DroneControlService.DRONE_EMERGENCY_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneBatteryReceiver, new IntentFilter(DroneControlService.DRONE_BATTERY_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneFlyingStateReceiver, new IntentFilter(DroneControlService.DRONE_FLYING_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneCameraReadyChangedReceiver, new IntentFilter(DroneControlService.CAMERA_READY_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneRecordReadyChangeReceiver, new IntentFilter(DroneControlService.RECORD_READY_CHANGED_ACTION));
    }

    private void unregisterReceivers()
    {
        // Unregistering system receiver
        unregisterReceiver(wifiSignalReceiver);

        // Unregistering local receivers
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(videoRecordingStateReceiver);
        localBroadcastMgr.unregisterReceiver(droneEmergencyReceiver);
        localBroadcastMgr.unregisterReceiver(droneBatteryReceiver);
        localBroadcastMgr.unregisterReceiver(droneFlyingStateReceiver);
        localBroadcastMgr.unregisterReceiver(droneCameraReadyChangedReceiver);
        localBroadcastMgr.unregisterReceiver(droneRecordReadyChangeReceiver);
    }

    @Override
    protected void onResume()
    {
        if (view != null) {
            view.onResume();
        }

        if (droneControlService != null) {
            droneControlService.resume();
        }

        registerReceivers();
        refreshWifiSignalStrength();

        // Start tracking device orientation
        deviceOrientationManager.resume();
        magnetoAvailable = deviceOrientationManager.isMagnetoAvailable();

        super.onResume();
    }

    @Override
    protected void onPause()
    {
        if (view != null) {
            view.onPause();
        }

        if (droneControlService != null) {
            droneControlService.pause();
        }

        unregisterReceivers();

        // Stop tracking device orientation
        deviceOrientationManager.pause();


        System.gc();
        super.onPause();
    }

    /**
     * Called when we connected to DroneControlService
     */
    protected void onDroneServiceConnected()
    {
        if (droneControlService != null) {
            droneControlService.resume();
            droneControlService.requestDroneStatus();
        } else {
            Log.w(TAG, "DroneServiceConnected event ignored as DroneControlService is null");
        }

        settingsDialog = new SettingsDialog(this, this, droneControlService, magnetoAvailable);

        applySettings(settings);

        runTranscoding();


    }


    @Override
    public void onDroneFlyingStateChanged(boolean flying)
    {
        this.flying = flying;

    }



    protected void onNotifyLowDiskSpace()
    {
        showWarningDialog(getString(R.string.your_device_is_low_on_disk_space), WARNING_MESSAGE_DISMISS_TIME);
    }


    protected void onNotifyLowUsbSpace()
    {
        showWarningDialog(getString(R.string.USB_drive_full_Please_connect_a_new_one), WARNING_MESSAGE_DISMISS_TIME);
    }


    protected void onNotifyNoMediaStorageAvailable()
    {
        showWarningDialog(getString(R.string.Please_insert_a_SD_card_in_your_Smartphone), WARNING_MESSAGE_DISMISS_TIME);
    }


    public void onCameraReadyChanged(boolean ready)
    {
        cameraReady = ready;

    }




    public void onDroneRecordVideoStateChanged(boolean recording, boolean usbActive, int remaining)
    {
        if (droneControlService == null)
            return;

        prevRecording = this.recording;
        this.recording = recording;


        if (!recording) {
            if (prevRecording != recording && droneControlService != null
                    && droneControlService.getDroneVersion() == EDroneVersion.DRONE_1) {
                runTranscoding();
                showWarningDialog(getString(R.string.Your_video_is_being_processed_Please_do_not_close_application), WARNING_MESSAGE_DISMISS_TIME);
            }
        }

        if (prevRecording != recording) {
            if (usbActive && droneControlService.getDroneConfig().isRecordOnUsb() && remaining == 0) {
                onNotifyLowUsbSpace();
            }
        }
    }

    protected void showSettingsDialog()
    {


        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("settings");

        if (prev != null) {
            return;
        }

        ft.addToBackStack(null);

        settingsDialog.show(ft, "settings");

        if (pauseVideoWhenOnSettings) {
            view.onPause();
        }
    }

    @Override
    public void onBackPressed()
    {
        if (canGoBack()) {
            super.onBackPressed();
        }
    }


    private boolean canGoBack()
    {
        return !((flying || recording || !cameraReady) && controlLinkAvailable);
    }



    private void applySettings(ApplicationSettings settings)
    {
        applySettings(settings, false);
    }

    private void applySettings(ApplicationSettings settings, boolean skipJoypadConfig)
    {
        magnetoEnabled = settings.isAbsoluteControlEnabled();

        if (magnetoEnabled) {
            if (droneControlService.getDroneVersion() == EDroneVersion.DRONE_1 || !deviceOrientationManager.isMagnetoAvailable() || NookUtils.isNook()) {
                // Drone 1 doesn't have compass, so we need to switch magneto
                // off.
                magnetoEnabled = false;
                settings.setAbsoluteControlEnabled(false);
            }
        }

        if (droneControlService != null)
            droneControlService.setMagnetoEnabled(magnetoEnabled);


    }

    private ApplicationSettings getSettings()
    {
        return ((FreeFlightApplication) getApplication()).getAppSettings();
    }

    public void refreshWifiSignalStrength()
    {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int signalStrength = WifiManager.calculateSignalLevel(manager.getConnectionInfo().getRssi(), 4);
        onWifiSignalStrengthChanged(signalStrength);
    }

    private void showWarningDialog(final String message, final int forTime)
    {
        final String tag = message;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(tag);

        if (prev != null) {
            return;
        }

        ft.addToBackStack(null);

        // Create and show the dialog.
        WarningDialog dialog = new WarningDialog();

        dialog.setMessage(message);
        dialog.setDismissAfter(forTime);
        dialog.show(ft, tag);
    }



    private void runTranscoding()
    {
        if (droneControlService.getDroneVersion() == EDroneVersion.DRONE_1) {
            File mediaDir = droneControlService.getMediaDir();

            if (mediaDir != null) {
                Intent transcodeIntent = new Intent(this, TranscodingService.class);
                transcodeIntent.putExtra(TranscodingService.EXTRA_MEDIA_PATH, mediaDir.toString());
                startService(transcodeIntent);
            } else {
                Log.d(TAG, "Transcoding skipped SD card is missing.");
            }
        }
    }

    public void onDeviceOrientationChanged(float[] orientation, float magneticHeading, int magnetoAccuracy)
    {
        if (droneControlService == null) {
            return;
        }

        if (magnetoEnabled && magnetoAvailable) {
            float heading = magneticHeading * 57.2957795f;

            if (screenRotationIndex == 1) {
                heading += 90.f;
            }

            droneControlService.setDeviceOrientation((int) heading, 0);
        } else {
            droneControlService.setDeviceOrientation(0, 0);
        }

        if (running == false) {
            pitchBase = orientation[PITCH];
            rollBase = orientation[ROLL];
            droneControlService.setPitch(0);
            droneControlService.setRoll(0);
        } else {

            float x = (orientation[PITCH] - pitchBase);
            float y = (orientation[ROLL] - rollBase);

            if (screenRotationIndex == 0) {
                // Xoom
                if (acceleroEnabled && (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD)) {
                    x *= -1;
                    droneControlService.setPitch(x);
                    droneControlService.setRoll(y);
                }
            } else if (screenRotationIndex == 1) {
                if (acceleroEnabled && (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD)) {
                    x *= -1;
                    y *= -1;

                    droneControlService.setPitch(y);
                    droneControlService.setRoll(x);
                }
            } else if (screenRotationIndex == 3) {
                // google tv
                if (acceleroEnabled && (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD)) {

                    droneControlService.setPitch(y);
                    droneControlService.setRoll(x);
                }
            }
        }
    }

    public void prepareDialog(SettingsDialog dialog)
    {
        dialog.setAcceleroAvailable(deviceOrientationManager.isAcceleroAvailable());

        if (NookUtils.isNook()) {
            // System see the magnetometer but actually it is not functional. So
            // we need to disable magneto
            dialog.setMagnetoAvailable(false);
        } else {
            dialog.setMagnetoAvailable(deviceOrientationManager.isMagnetoAvailable());
        }

        dialog.setFlying(flying);
        dialog.setConnected(controlLinkAvailable);
        dialog.enableAvailableSettings();
    }


    @Override
    public void onOptionChangedApp(SettingsDialog dialog, EAppSettingProperty property, Object value)
    {
        if (value == null || value == null) {
            throw new IllegalArgumentException("Property can not be null");
        }

        ApplicationSettings appSettings = getSettings();

        switch (property) {
            case LEFT_HANDED_PROP:
            case MAGNETO_ENABLED_PROP:
            case CONTROL_MODE_PROP:

                break;
            case INTERFACE_OPACITY_PROP:
                if (value instanceof Integer) {
                }
                break;
            default:
                // Ignoring any other option change. They should be processed in onDismissed

        }
    }


    @Override
    public void onDismissed(SettingsDialog settingsDialog)
    {

        // pauseVideoWhenOnSettings option is not mandatory and is set depending to device in config.xml.
        if (pauseVideoWhenOnSettings) {
            view.onResume();
        }

        AsyncTask<Integer, Integer, Boolean> loadPropTask = new AsyncTask<Integer, Integer, Boolean>()
        {
            @Override
            protected Boolean doInBackground(Integer... params)
            {
                // Skipping joypad configuration as it was already done in onPropertyChanged
                // We do this because on some devices joysticks re-initialization takes too
                // much time.
                applySettings(getSettings(), true);
                return Boolean.TRUE;
            }

            @Override
            protected void onPostExecute(Boolean result)
            {

            }
        };

        loadPropTask.execute();
    }


    private boolean isLowOnDiskSpace()
    {
        boolean lowOnSpace = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            DroneConfig config = droneControlService.getDroneConfig();
            if (!recording && !config.isRecordOnUsb()) {
                File mediaDir = droneControlService.getMediaDir();
                long freeSpace = 0;

                if (mediaDir != null) {
                    freeSpace = mediaDir.getUsableSpace();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
                        && freeSpace < LOW_DISK_SPACE_BYTES_LEFT) {
                    lowOnSpace = true;
                }
            }
        } else {
            // TODO: Provide alternative implementation. Probably using StatFs
        }

        return lowOnSpace;
    }






    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus)
            setFullscreenMode();
        //what
    }

    private void setFullscreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(5894);
    }


    @Override
    public void onDroneBatteryChanged(int value) {

    }

    @Override
    public void onDroneEmergencyChanged(int code) {

    }

    @Override
    public void onDroneRecordReadyChanged(boolean ready) {

    }

    @Override
    public void onWifiSignalStrengthChanged(int strength) {

    }
}

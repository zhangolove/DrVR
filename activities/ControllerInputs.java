package com.parrot.freeflight.activities;

import android.annotation.SuppressLint;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.app.Activity;

import com.parrot.freeflight.service.DroneControlService;

import java.util.ArrayList;

/**
 * Created by User on 3/16/2015.
 */
public class ControllerInputs extends Activity{

    private boolean combinedYawEnabled = true;
    private boolean progressiveCommandEnabled = false;

    @SuppressLint("NewApi")
    public ArrayList getGameControllerIds() {
        ArrayList gameControllerDeviceIds = new ArrayList();
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();

            // Verify that the device has gamepad buttons, control sticks, or both.
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                    || ((sources & InputDevice.SOURCE_JOYSTICK)
                    == InputDevice.SOURCE_JOYSTICK)) {
                // This device is a game controller. Store its device ID.
                if (!gameControllerDeviceIds.contains(deviceId)) {
                    gameControllerDeviceIds.add(deviceId);
                }
            }
        }
        return gameControllerDeviceIds;
    }

    @SuppressLint("NewApi")
    private static float getCenteredAxis(MotionEvent event,
                                         InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range =
                device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = (float) .35;
            final float value =
                    historyPos < 0 ? event.getAxisValue(axis):
                            event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    @SuppressLint("NewApi")
    public void processJoystickInput(MotionEvent event,
                                     int historyPos, DroneControlService droneControlService) {

        InputDevice mInputDevice = event.getDevice();

        // Calculate the horizontal distance to move by
        // using the input value from one of these physical controls:
        // the left control stick, hat axis, or the right control stick.
        float lx = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_X, historyPos);
        float rx = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Z, historyPos);
//	    if (lx == 0) {
//	        lx = getCenteredAxis(event, mInputDevice,
//	                MotionEvent.AXIS_HAT_X, historyPos);
//	    }

        // Calculate the vertical distance to move by
        // using the input value from one of these physical controls:
        // the left control stick, hat switch, or the right control stick.
        float ly = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Y, historyPos);
        float ry = getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_RZ, historyPos);
//	    if (ly == 0) {yea
//	        ly = getCenteredAxis(event, mInputDevice,
//	                MotionEvent.AXIS_HAT_Y, historyPos);
//	    }

	if (droneControlService != null){
		
		if (lx ==0 && ly ==0 && progressiveCommandEnabled){
		    droneControlService.setProgressiveCommandEnabled(false);
		    progressiveCommandEnabled = false;
		    //droneControlService.setProgressiveCommandCombinedYawEnabled(false);
		}else if (rx == 0 && ry == 0 && lx ==0 && ly ==0 && combinedYawEnabled){
		    combinedYawEnabled = false;
		    droneControlService.setProgressiveCommandCombinedYawEnabled(false);
		    if (progressiveCommandEnabled){
			droneControlService.setProgressiveCommandEnabled(true);
		    }
		}else if (!(lx ==0 && ly ==0) && !progressiveCommandEnabled){
		    droneControlService.setProgressiveCommandEnabled(true);
		    progressiveCommandEnabled=true;
		    //droneControlService.setProgressiveCommandCombinedYawEnabled(true);
		}else if (!(rx == 0 && ry == 0) && !combinedYawEnabled){
		    combinedYawEnabled = true;
		    progressiveCommandEnabled = true;
		    droneControlService.setProgressiveCommandCombinedYawEnabled(true);  
		}
		
		//droneControlService.setProgressiveCommandEnabled(true);
		//droneControlService.setProgressiveCommandCombinedYawEnabled(true);

		droneControlService.setGaz((byte) -ry);
		droneControlService.setYaw((byte) rx);
		droneControlService.setPitch((byte) ly);
		droneControlService.setRoll((byte) lx);
	}

    }

    public boolean processKeyDown(int keyCode, KeyEvent event, DroneControlService droneControlService) {
        boolean handled = false;
        switch (keyCode) {
            // Handle gamepad and D-pad button presses to
            // navigate the ship
            case KeyEvent.KEYCODE_BUTTON_A:
                if (droneControlService != null) {
                    droneControlService.triggerTakeOff();
                    handled = true;
                }
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
                if (droneControlService != null) {
                    droneControlService.triggerTakeOff();
                    handled = true;
                }
                break;
            default:
                break;
        }
        if (handled) {
            return true;
        }
        return false;
    }

}

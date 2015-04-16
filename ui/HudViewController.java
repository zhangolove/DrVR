/*
 * HudViewController
 *
 *  Created on: July 5, 2011
 *      Author: Dmytro Baryskyy
 */

package com.parrot.freeflight.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import com.parrot.freeflight.R;
import com.parrot.freeflight.drone.NavData;
import com.parrot.freeflight.gestures.EnhancedGestureDetector;
import com.parrot.freeflight.ui.hud.Button;
import com.parrot.freeflight.ui.hud.Image;
import com.parrot.freeflight.ui.hud.Image.SizeParams;
import com.parrot.freeflight.ui.hud.Indicator;
import com.parrot.freeflight.ui.hud.JoystickBase;
import com.parrot.freeflight.ui.hud.Sprite;
import com.parrot.freeflight.ui.hud.Sprite.Align;
import com.parrot.freeflight.ui.hud.Text;
import com.parrot.freeflight.ui.hud.ToggleButton;
import com.parrot.freeflight.utils.FontUtils.TYPEFACE;
import com.parrot.freeflight.video.VideoStageRenderer;
import com.parrot.freeflight.video.VideoStageView;

public class HudViewController
{




    private GLSurfaceView glView;




    private VideoStageRenderer renderer;
    private Activity context;

    private boolean useSoftwareRendering;
    private int prevRemainingTime;

    private SparseIntArray emergencyStringMap;

    public HudViewController(Activity context, boolean useSoftwareRendering)
    {

        this.context = context;
        this.useSoftwareRendering = useSoftwareRendering;



        glView = new GLSurfaceView(context);
        glView.setEGLContextClientVersion(2);

        context.setContentView(glView);

        renderer = new VideoStageRenderer(context, null);

        initGLSurfaceView();

        Resources res = context.getResources();



        Image topBarBg = new Image(res, R.drawable.barre_haut, Align.TOP_CENTER);
        topBarBg.setSizeParams(SizeParams.FILL_SCREEN, SizeParams.NONE);
        topBarBg.setAlphaEnabled(false);






    }




    private void initGLSurfaceView() {
        if (glView != null) {
            glView.setRenderer(renderer);

        }
    }



    public void onPause()
    {
        if (glView != null) {
            glView.onPause();
        }


    }


    public void onResume()
    {
        if (glView != null) {
            glView.onResume();
        }

    }







    public View getRootView()
    {
        if (glView != null) {
            return glView;
        }
        return null;
    }



}

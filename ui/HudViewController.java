/*
 * HudViewController
 *
 *  Created on: July 5, 2011
 *      Author: Dmytro Baryskyy
 */

package com.parrot.freeflight.ui;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.view.View;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.parrot.freeflight.video.VideoStageRenderer;


public class HudViewController
{




    private CardboardView glView;




    private VideoStageRenderer renderer;
    private Activity context;


    public HudViewController(Activity context, boolean useSoftwareRendering)
    {

        this.context = context;




        glView = new CardboardView(context);
        glView.setEGLContextClientVersion(2);

        context.setContentView(glView);

        renderer = new VideoStageRenderer(context, null);

        initGLSurfaceView();





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

package com.parrot.freeflight.ui.gl;

import javax.microedition.khronos.opengles.GL10;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;

public class GLBGVideoSprite extends GLSprite
{
    private static final String TAG = GLBGVideoSprite.class.getSimpleName();

    private android.graphics.Matrix matrix;

    private float videoSize[]  = {0,0, //image width, height
            0,0}; //
    public int screenWidth;
    public int screenHeight;
    private int videoWidth;
    private int videoHeight;
    private boolean isVideoReady = false;
    private boolean frameDrawn = false;

    private int prevImgWidth;
    private int prevImgHeight;

    private int x;
    private int y;


    public GLBGVideoSprite(Resources resources)
    {
        super(resources, null);

    }


    @Override
    protected void onUpdateTexture()
    {
        if (onUpdateVideoTextureNative(program, textures[0]) && (prevImgWidth != imageWidth || prevImgHeight != imageHeight)) {
            if (!isVideoReady) {
                isVideoReady = true;
            }

            float coef = ((float)screenWidth / (float)imageWidth);

            setSize((int)(imageWidth * coef), (int)(imageHeight * coef));
            x = (screenWidth - width) / 2;
            y = (screenHeight - height) / 2;

            prevImgWidth = imageWidth;
            prevImgHeight = imageHeight;
        }
    }


    @Override
    public void onSurfaceChanged(int width, int height)
    {
        this.screenWidth = width;
        this.screenHeight = height;

        onSurfaceChangedNative(width, height);

        super.onSurfaceChanged(width, height);
    }


    @Override
    public void onDraw(float x, float y)
    {   if (!isVideoReady) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

        super.onDraw(this.x, this.y);
    }






    private native boolean onUpdateVideoTextureNative(int program, int textureId);
    private native void onSurfaceCreatedNative();
    private native void onSurfaceChangedNative(int width, int height);
    private native boolean getVideoFrameNative(Bitmap bitmap, float[] ret);
}

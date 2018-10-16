package org.martinrgb.hiframeanimationlib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;


final public class FrameDrawable {
    private static final String TAG = FrameDrawable.class.getSimpleName();

    private String mDrawableResPath;
    private Matrix mMatrix;
    private Paint mPaint;
    private boolean mMiXAlpha = false;

    int mixAlpha = 255;
    float mX;
    float mY;
    float mScale = 1.0f;
    float mAlpha = 1.0f;
    long mDuration; //当前帧持续显示时间

    public FrameDrawable(String drawableResPath, long duration) {
        mDrawableResPath = drawableResPath;
        mDuration = duration;
        mMatrix = new Matrix();
        mPaint = new Paint();
    }

    public void setMixAlpha(boolean boo){
        mMiXAlpha = boo;
        if (boo == true){
            mixAlpha = 122;
        }
        else{
            mixAlpha = 255;
        }
    }

    public boolean ismMiXAlpha() {
        return mMiXAlpha;
    }

    public Bitmap draw(Canvas canvas, long start) {
        //这里是从assets中获取bitmap，当然也可以从sdcard中获取，这样就可以热更了帧动画了
        final Bitmap frameBitmap = Utils.loadBitmap(mDrawableResPath);
        if (null != frameBitmap) {
            final float scaleX = (float) canvas.getWidth() / frameBitmap.getWidth();
            final float scaleY = (float) canvas.getHeight() / frameBitmap.getHeight();
            mMatrix.setTranslate(mX, mY);
            mMatrix.preScale(scaleX * mScale, scaleY * mScale, 0, 0);
            mPaint.setAlpha((int) (mAlpha * mixAlpha));
            canvas.drawBitmap(frameBitmap, mMatrix, mPaint);
        }
        return frameBitmap;
    }

}

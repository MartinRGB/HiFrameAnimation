package org.martinrgb.hiframeanimationlib;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FrameAnimationView extends FrameSurfaceView {
    private static final String TAG = FrameAnimationView.class.getSimpleName();
    private int mCurFrame = -1;


    private long mStart;
    private long mDuration;
    private int mCurRepeats;
    public int mControlFrame = 0;
    private boolean mIsControl = false;
    private boolean mIsRepeat = false;

    private AtomicBoolean mIsAnimating = new AtomicBoolean(false);

    private List<FrameDrawable> mFrameDrawables = new ArrayList<>();
    private OnFrameListener mOnFrameListener;

    public interface OnFrameListener {

        /**
         * 第一帧开始绘制的时候会回调
         */
        void onFrameStart();

        /**
         * 如果是oneShot的时候会回调，否则只用调用stop的适合才会回调
         */
        void onFrameEnd();

        void onFrameUpdate(int currentFrame);

        void onFramePause();

    }

    public FrameAnimationView(Context context) {
        this(context, null);
    }

    public FrameAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * 是否播放一次，否则就循环播放
     *
     * @param repeat
     */
    public void setRepeat(boolean repeat) {
        if (!isRunning()) {
            mIsRepeat = repeat;
        }
    }

    public void setIsControl(boolean isControl) {
        if (!isRunning()) {
            mIsControl = isControl;
        }
    }

    /**
     * 动画播放时长，如果不设置，就是所有帧的时间总和
     *
     * @param duration
     */
    public void setDuration(long duration) {
        if (!isRunning()) {
            mDuration = duration;
        }
    }

    public void addFrameDrawable(FrameDrawable frameDrawable) {
        if (!isRunning()) { //在绘制的时候不允许添加
            mFrameDrawables.add(frameDrawable);
        }
    }

    public void addFrameDrawable(List<FrameDrawable> frameDrawableList) {
        if (!isRunning()) {
            mFrameDrawables.clear();
            mFrameDrawables.addAll(frameDrawableList);
        }
    }

    public void setOnFrameListener(OnFrameListener onFrameListener) {
        mOnFrameListener = onFrameListener;
    }

    private void callOnFrameEnd() {
        if (null != mOnFrameListener) {
            mOnFrameListener.onFrameEnd();
        }
    }

    private void callOnFrameStart() {
        if (null != mOnFrameListener) {
            mOnFrameListener.onFrameStart();
        }
    }

    private void callOnFrameUpdate(int currentFrame) {
        if (null != mOnFrameListener) {
            mOnFrameListener.onFrameUpdate(currentFrame);
        }
    }


    private void callOnFramePause() {
        if (null != mOnFrameListener) {
            mOnFrameListener.onFramePause();
        }
    }

    /**
     * 是否在播放帧动画
     *
     * @return
     */
    public boolean isAnimating() {
        return mIsAnimating.get();
    }

    @Override
    protected void startUpdate() {
        if (isRunning()) return;
        if (mFrameDrawables.isEmpty()) {
            callOnFrameEnd();
            return;
        }
        if (mDuration == 0) {
            for (FrameDrawable frameDrawable : mFrameDrawables) {
                if (null != frameDrawable) {
                    mDuration += frameDrawable.mDuration;
                }
            }
        }
        if (mDuration == 0) {
            callOnFrameEnd();
            return;
        }
        super.startUpdate();
    }

    @Override
    protected void stopUpdate() {
        if (mIsAnimating.get()) {
            mIsAnimating.set(false);
            callOnFrameEnd();
        }
        mCurFrame = -1;
        super.stopUpdate();
    }

    @Override
    protected void pauseUpdate() {
        if (mIsAnimating.get()) {
            mIsAnimating.set(false);
            callOnFramePause();
        }
        super.pauseUpdate();
    }

    /**
     * 绘制的逻辑
     *
     * @param canvas
     */
    @Override
    protected void drawFrame(Canvas canvas) {
        final int numFrames = mFrameDrawables.size();
        final int lastFrame = numFrames - 1;
        final long curTime = SystemClock.uptimeMillis();

        // End
        if (mStart != 0 && curTime - mStart >= mDuration) {
            if (!mIsRepeat && mIsAnimating.get()) {
                mIsAnimating.set(false);
                post(new Runnable() {
                    @Override
                    public void run() {
                        callOnFrameEnd();
                    }
                });
                mStart = 0;
                mCurRepeats = 0;
            }
        }

        // Loop
        int nextFrame = mCurFrame + 1;
        if (!mIsRepeat && nextFrame > lastFrame) {
            nextFrame = lastFrame;
        }
        if (mIsRepeat && nextFrame >= numFrames) {
            nextFrame = lastFrame;
            if (mStart != 0 && curTime - mStart >= mDuration) {
                nextFrame = 0;
            }
        }





        callOnFrameUpdate(mCurFrame+1);

        // Start
        if (nextFrame == 0) { //第一帧的时候开始记录时间
            mIsAnimating.set(true);
            mStart = curTime;
            if (++mCurRepeats == 1) {//第一次播放动画的时候回调
                post(new Runnable() {
                    @Override
                    public void run() {
                        callOnFrameStart();
                    }
                });
            }
        }
        mCurFrame = nextFrame;

        if(mIsControl){

            drawNext(canvas, mControlFrame, curTime);
        }
        else {

            drawNext(canvas, nextFrame, curTime);
        }
    }

    /**
     * @param canvas
     * @param nextFrame
     * @param start     当前帧开始绘制的时间
     */
    public void drawNext(Canvas canvas, int nextFrame, long start) {
        long frameDuration = 0;
        FrameDrawable frameDrawable = mFrameDrawables.get(nextFrame);

        if (null != frameDrawable) {
            frameDuration = frameDrawable.mDuration;
            if(!frameDrawable.ismMiXAlpha()){
                clearCanvas(canvas);
            }
            frameDrawable.draw(canvas, start);
        }
        final long cost = SystemClock.uptimeMillis() - start;
        //Log.d(TAG, "Updating,frame cost :" + cost);
        if (frameDuration > cost) {
            try {
                Thread.sleep(frameDuration - cost);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

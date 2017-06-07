package com.martinrgb.livewallpapertemplate.frameutil;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrameSurfaceView extends GLSurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = FrameSurfaceView.class.getSimpleName();

    //###################### Setting ######################

    private static final int MIN_UPDATE_RATE = 8;
    private int mCurFrame = -1;
    private static final String FRAME_NAME = "testframe";
    private boolean isOneShot = false;
    public boolean isControl = false;
    public int mControlFrame = 0; //可以用Spring，结合滑动位置控制


    //###################### Listener ######################
    private OnFrameListener mOnFrameListener;
    public interface OnFrameListener {

        void onFrameStart();
        void onFrameEnd();

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

    //### Animating Or Not
    private AtomicBoolean mIsAnimating = new AtomicBoolean(false);
    public boolean isAnimating() {
        return mIsAnimating.get();
    }


    //###################### Init ######################
    private boolean mOneShot;
    private long mStart;
    private long mDuration;
    private List<FrameDrawable> mFrameDrawables = new ArrayList<>();
    private volatile boolean mIsSurfaceCreated;
    private volatile int mSurfaceWidth;
    private volatile int mSurfaceHeight;
    private SurfaceHolder mHolder = null;

    public FrameSurfaceView(Context context) {
        super(context, null);
        initView();
        addAsset(context);

    }

    public FrameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        addAsset(context);
    }

    private void initView() {
        if (!isInEditMode()) {
            setZOrderMediaOverlay(true);
            setZOrderOnTop(false);
        }
        setWillNotCacheDrawing(true);
        setDrawingCacheEnabled(false);
        setWillNotDraw(true);
//        if(mHolder == null){
//            mHolder = getHolder();
//        }
//        else {
//            //
//        }
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        getHolder().addCallback(this);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mIsSurfaceCreated = true;
        clearSurface();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mIsSurfaceCreated = true;
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsSurfaceCreated = false;
    }


    private void addAsset(Context context){
        List<String> frameList = null;
        try {
            final String[] frames = context.getAssets().list(FRAME_NAME);
            if (null != frames) {
                frameList = Arrays.asList(frames);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //按帧图片的序列号排序
        if (null != frameList) {
            Collections.sort(frameList, new Comparator<String>() {

                //根据情况修改
                private final String MATCH_FRAME_NUM = String.format("(?<=%s_).*(?=.jpg)", FRAME_NAME);
                private final Pattern p = Pattern.compile(MATCH_FRAME_NUM);

                @Override
                public int compare(String lhs, String rhs) {
                    try {
                        final Matcher lhsMatcher = p.matcher(lhs);
                        final Matcher rhsMatcher = p.matcher(rhs);
                        if (lhsMatcher.find()
                                && rhsMatcher.find()) {
                            return Integer.valueOf(lhsMatcher.group()) - Integer.valueOf(rhsMatcher.group());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 0;
                }
            });
            //添加序列帧
            List<FrameDrawable> frameDrawables = new ArrayList<>();
            for (String framePath : frameList) {
                FrameDrawable frameDrawable = new FrameDrawable(FRAME_NAME + "/" + framePath, 8,context);
                frameDrawables.add(frameDrawable);

            }

            addFrameDrawable(frameDrawables); //添加序列帧
            if(isOneShot){
                setOneShot(true);
            }
            else {
                setOneShot(false);
            }

            start();
        }
    }


    public void setOneShot(boolean oneShot) {
        if (!isRunning()) {
            mOneShot = oneShot;
        }
    }

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

    //###################### Start & Stop ######################
    private int mFrameUpdateRate = MIN_UPDATE_RATE;
    private UpdateThread mUpdateThread;
    private boolean mIsUpdateStarted;

    public synchronized void start() {
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
        startUpdate();
    }

    public synchronized void stop() {
        if (mIsAnimating.get()) {
            mIsAnimating.set(false);
            callOnFrameEnd();
        }
        mCurFrame = -1;
        stopUpdate();
    }

    public boolean isRunning() {
        return mIsUpdateStarted;
    }

    protected void startUpdate() {
        if (mIsUpdateStarted) return;
        mUpdateThread = new UpdateThread("Animator Update Thread") {

            @Override
            public void run() {
                try {
                    while (!isQuited()
                            && !Thread.currentThread().isInterrupted()) {
                        long drawTime = drawSurface();
                        long diffTime = mFrameUpdateRate - drawTime;
                        if (isQuited()) {
                            break;
                        }
                        if (diffTime > 0) {
                            SystemClock.sleep(diffTime);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        mIsUpdateStarted = true;
        mUpdateThread.start();
    }

    protected void stopUpdate() {
        mIsUpdateStarted = false;
        if (null != mUpdateThread) {
            UpdateThread updateThread = mUpdateThread;
            mUpdateThread = null;
            updateThread.quit();
            updateThread.interrupt();
        }
    }

    //###################### Draw ######################
    private static RectF RECT = new RectF();
    private static Paint PAINT = new Paint();
    private int mCurRepeats;
    static {
        PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        PAINT.setColor(Color.TRANSPARENT);
    }

    final protected long drawSurface() {
        if (!mIsSurfaceCreated) {
            return 0;
        }
        if (mSurfaceWidth == 0
                || mSurfaceHeight == 0) {
            return 0;
        }
        if (!isShown()) {
            clearSurface();
            return 0;
        }
        final long startTime = SystemClock.uptimeMillis();
        if (mIsSurfaceCreated) {
            Canvas canvas = getHolder().lockCanvas();
            if (null != canvas) {
                drawFrame(canvas);
                if (mIsSurfaceCreated) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
        return SystemClock.uptimeMillis() - startTime;
    }

    protected void drawFrame(Canvas canvas){
        final int numFrames = mFrameDrawables.size();
        final int lastFrame = numFrames - 1;
        final long curTime = SystemClock.uptimeMillis();

        if (mStart != 0
                && curTime - mStart >= mDuration) {
            if (mOneShot
                    && mIsAnimating.get()) {
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
        int nextFrame = mCurFrame + 1;
        if (mOneShot && nextFrame > lastFrame) {
            nextFrame = lastFrame;
        }
        if (!mOneShot && nextFrame >= numFrames) {
            nextFrame = lastFrame;
            if (mStart != 0
                    && curTime - mStart >= mDuration) {
                nextFrame = 0;
            }
        }
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

        if(isControl){

            drawNext(canvas, mControlFrame, curTime);
        }
        else {

            drawNext(canvas, nextFrame, curTime);
        }
    };

    public void drawNext(Canvas canvas, int nextFrame, long start) {
        long frameDuration = 0;
        FrameDrawable frameDrawable = mFrameDrawables.get(nextFrame);
        if (null != frameDrawable) {
            frameDuration = frameDrawable.mDuration;
            clearCanvas(canvas);
            frameDrawable.draw(canvas, start);
        }
        final long cost = SystemClock.uptimeMillis() - start;
        Log.d(TAG, "frame cost :" + cost);
        if (frameDuration > cost) {
            try {
                Thread.sleep(frameDuration - cost);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    final protected void clearSurface() {
        if (mIsSurfaceCreated) {
            Canvas canvas = getHolder().lockCanvas();
            if (null != canvas) {
                clearCanvas(canvas);
                if (mIsSurfaceCreated) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    final protected void clearCanvas(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT,
                PorterDuff.Mode.CLEAR);
        RECT.set(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawRect(RECT, PAINT);
    }



}

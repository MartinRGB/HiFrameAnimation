package org.martinrgb.hiframeanimation;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;
import com.zhy.autolayout.AutoLayoutActivity;

import org.martinrgb.hiframeanimationlib.FrameAnimationView;
import org.martinrgb.hiframeanimationlib.FrameDrawable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControlAcitivity extends AutoLayoutActivity {
    private static final String TAG = ControlAcitivity.class.getSimpleName();
    private FrameAnimationView mFrameAnimationView;
    private ImageView gestureDetectView;
    private static final String FRAME_NAME = "trans";
    private boolean isControl = true;
    private boolean isRepeat = false;
    private boolean isSpring = false;
    private int FPS = 60;
    private int frameNumber = 293;
    private ContinueUpdater mUpdate;
    private ContinueUpdater.UpdateListener updateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deleteBars();
        setContentView(R.layout.activity_frame);
        setSpringSystem();
        initTouchListener();
        if(!isSpring){
            setUpdateListener();
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initFrameView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFrameAnimationView.stop(); //停止播放
        mFrameAnimationView.setOnFrameListener(null); //移除监听器
    }


    private void initFrameView(){
        mFrameAnimationView = (FrameAnimationView) findViewById(R.id.frame_animation);
        gestureDetectView = (ImageView) findViewById(R.id.gesture_detect);
        List<String> frameList = null;
        try {
            final String[] frames = getAssets().list(FRAME_NAME);
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
                FrameDrawable frameDrawable = new FrameDrawable(FRAME_NAME + "/" + framePath, 16);
                frameDrawables.add(frameDrawable);
            }

            mFrameAnimationView.addFrameDrawable(frameDrawables); //添加序列帧
            mFrameAnimationView.setOnFrameListener(new FrameAnimationView.OnFrameListener() { //添加监听器
                @Override
                public void onFrameStart() {
                }

                @Override
                public void onFrameUpdate(int currentFrame) {
                    if(currentFrame == 59 && !isControl){
                        //You can stop frame anim here
                    }
                }

                @Override
                public void onFrameEnd() {
                }

                @Override
                public void onFramePause() {
                }
            });

            if(isRepeat){
                mFrameAnimationView.setRepeat(true);
            }else{
                mFrameAnimationView.setRepeat(false);
            }

            if(isControl){
                mFrameAnimationView.setIsControl(true);
            }
            else {
                mFrameAnimationView.setIsControl(false);
            }

            mFrameAnimationView.setFPS(FPS);

            mFrameAnimationView.start();
        }
    }

    //##################### Spring System Part ######################
    private SpringSystem mSpringSystem;
    private Spring mSpring;
    private static final SpringConfig mconfig = SpringConfig.fromOrigamiTensionAndFriction(100, 15);

    private void setSpringSystem() {
        mSpringSystem = SpringSystem.create();
        mSpring = mSpringSystem.createSpring();
        mSpring.setSpringConfig(mconfig);

        mSpring.addListener(new SimpleSpringListener() {

            @Override
            public void onSpringUpdate(Spring mSpring) {

                float value = (float) mSpring.getCurrentValue();
                float mapValue = (float) SpringUtil.mapValueFromRangeToRange(value, 0, 1, 0, frameNumber);
                int intMapValue = (int) mapValue;
                //mFrameAnimationView.drawNext();
                mFrameAnimationView.mControlFrame = clampFrame(intMapValue);


            }
        });
    }

    //##################### Touch Listener Part ######################
    private float mStartTouchEventY;
    private float mDistanceProgress = 0;
    private View.OnTouchListener advancedListener;

    private void initTouchListener(){

        advancedListener = new View.OnTouchListener() {
            //@Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        //确定一下是否为短时间Tap,tap则开关屏幕
                        mDistanceProgress = 0;
                        mStartTouchEventY = event.getY(0);
                        gestureDetectView.setAlpha(1.f);
                        gestureDetectView.setTranslationY(mStartTouchEventY - 330);
                        break;
                    case MotionEvent.ACTION_UP:
                        //unlock判定
                        gestureDetectView.setAlpha(0.f);
                        if(mDistanceProgress>0.6){
                            if(isSpring){
                                mSpring.setEndValue(1.);
                            }
                            else {
                                //mFrameAnimationView.mControlFrame = frameNumber;
                                mUpdate = new ContinueUpdater(mFrameAnimationView.mControlFrame,frameNumber,updateListener,FPS*2);
                                mUpdate.run();
                            }
                        }
                        else{
                            if(isSpring){
                                mSpring.setEndValue(0);
                            }
                            else{
                                //mFrameAnimationView.mControlFrame = 0;
                                mUpdate = new ContinueUpdater(mFrameAnimationView.mControlFrame,0,updateListener,FPS*2);
                                mUpdate.run();
                            }
                        }

                        break;
                    case MotionEvent.ACTION_MOVE:

                        float nowDistanceY =  mStartTouchEventY - event.getY(0);
                        mDistanceProgress = Math.max(0,Math.min(1,nowDistanceY/550));
                        if(isSpring){
                            mSpring.setEndValue(mDistanceProgress);
                        }
                        else{
                            int intMapValue = (int) SpringUtil.mapValueFromRangeToRange(nowDistanceY, 0, 550, 0, frameNumber);
                            mFrameAnimationView.mControlFrame = clampFrame(intMapValue);
                        }

                        break;
                }
                return true;
            }
        };

        findViewById(R.id.firstscreen).setOnTouchListener(advancedListener);
    }

    private void setUpdateListener(){
        updateListener = new ContinueUpdater.UpdateListener() {
            @Override
            public void onUpdate(int frameNumber) {

                Log.e("frame",String.valueOf(frameNumber));
                mFrameAnimationView.mControlFrame =frameNumber;
            }

            @Override
            public void onEnd(){
                Log.e("isEnd","isEnd");
            }
        };
    }

    private int clampFrame(int val){
        return Math.max(0,Math.min(frameNumber,val));
    }

    //##################### Util ######################
    private void deleteBars(){
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            mFrameAnimationView.pause();
        }

        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            mFrameAnimationView.start();
        }

        if ((keyCode == KeyEvent.KEYCODE_BACK)){
            finish();
        }

        return true;
    }

}

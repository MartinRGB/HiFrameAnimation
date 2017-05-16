package org.martinrgb.hiframeanimation;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringUtil;
import com.zhy.autolayout.AutoLayoutActivity;

import org.martinrgb.hiframeanimationlib.FrameAnimationView;
import org.martinrgb.hiframeanimationlib.FrameDrawable;
import org.martinrgb.interpolator.InterpolatorAnimator;
import org.martinrgb.interpolator.InterpolatorConfig;
import org.martinrgb.interpolator.InterpolatorConfigRegistry;
import org.martinrgb.interpolator.InterpolatorConfigurationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AutoLayoutActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private FrameAnimationView mFrameAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        mFrameAnimationView = (FrameAnimationView) findViewById(R.id.frame_animation);
        setSpringSystem();
        initTouchListener();
        interpolatorConfig();
        animationInit();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFrameAnimationView.stop(); //停止播放
        mFrameAnimationView.setOnFrameListener(null); //移除监听器
    }


    //##################### 序列帧处理 ######################
    private static final String FRAME_NAME = "trans";
    private boolean userControl = true;
    private boolean isOneShot = false;

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
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
                    Log.d(TAG, "帧动画播放开始！");
                }

                @Override
                public void onFrameEnd() {
                    //mFrameAnimationView.mControlFrame = 293;
                    Log.d(TAG, "帧动画播放结束！");
                }
            });

            if(isOneShot){
                mFrameAnimationView.setOneShot(true); //循环播放帧动画
            }else{
                mFrameAnimationView.setOneShot(false);
            }

            if(userControl){

                mFrameAnimationView.isControl = true;
            }
            else {
                mFrameAnimationView.isControl = false;
            }



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

                float mapValue = (float) SpringUtil.mapValueFromRangeToRange(value, 0, 1, 0, 293);
                int intMapValue = (int) mapValue;

                //mFrameAnimationView.drawNext();
                mFrameAnimationView.mControlFrame = intMapValue;


            }
        });
    }

    //##################### Touch Listener Part ######################
    private float mStartTouchEventY;
    private float mDistanceProgress = 0;
    private View.OnClickListener clickListener;
    private View.OnTouchListener advancedListener;
    private boolean mIsOpened = false;
    private long downTime;
    private long upTime;

    private void initTouchListener(){

        clickListener =  new View.OnClickListener() {
            public void onClick(View v) {
                mIsOpened = !mIsOpened;
                if(mIsOpened){
                    transitionIn();

                    //######### 重启 #########
                    mFrameAnimationView.stop();
                    mFrameAnimationView.start(); //开始播放
                }else{
                    transitionOut();
                }
            }
        };

        findViewById(R.id.firstscreen).setOnClickListener(clickListener);

        advancedListener = new View.OnTouchListener() {
            //@Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        //确定一下是否为短时间Tap,tap则开关屏幕
                        downTime = System.currentTimeMillis();

                        mDistanceProgress = 0;
                        mStartTouchEventY = event.getY(0);

                        break;
                    case MotionEvent.ACTION_UP:

                        //确定一下是否为短时间Tap,tap则开关屏幕
                        upTime = System.currentTimeMillis();
                        if(upTime-downTime<=200){
                            findViewById(R.id.firstscreen).performClick();
                        }

                        //unlock判定
                        if(mDistanceProgress>0.6){
                            mSpring.setEndValue(1.);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                }
                            },100);

                        }
                        else{
                            mSpring.setEndValue(0);
                        }

                        break;
                    case MotionEvent.ACTION_MOVE:

                        float nowDistanceY =  mStartTouchEventY - event.getY(0);
                        mDistanceProgress = Math.max(0,Math.min(1,nowDistanceY/550));
                        mSpring.setEndValue((float) mDistanceProgress);

                        break;
                }
                return true;
            }
        };

        findViewById(R.id.firstscreen).setOnTouchListener(advancedListener);
    }

    //###################### 入场 & 退场动画初始化 ######################
    private InterpolatorConfigurationView mInterpolatorConfiguratorView;
    private InterpolatorConfig config0;
    private InterpolatorConfig config1;
    private InterpolatorConfig config2;
    private InterpolatorConfig config3;
    private InterpolatorConfig config4;
    private InterpolatorConfig config5;
    private InterpolatorConfig config6;
    private InterpolatorConfig config7;
    private void transitionIn(){

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                InterpolatorAnimator topScaleX = new InterpolatorAnimator(findViewById(R.id.timeLabel), "scaleX", 0.8f, 1f,config0.interpolator,config0.duration,config0.delayTime);
                InterpolatorAnimator topScaleY = new InterpolatorAnimator(findViewById(R.id.timeLabel), "scaleY", 0.8f, 1f,config0.interpolator,config0.duration,config0.delayTime);
                InterpolatorAnimator leftTransitionX = new InterpolatorAnimator(findViewById(R.id.leftLabel), "translationX", -100f, 0f,config1.interpolator,config1.duration,config1.delayTime);
                InterpolatorAnimator rightTransitionX = new InterpolatorAnimator(findViewById(R.id.rightLabel), "translationX", 100f, 0f,config1.interpolator,config1.duration,config1.delayTime);
                InterpolatorAnimator middleTransitionY = new InterpolatorAnimator(findViewById(R.id.middleLabel), "translationY", 100f, 0f,config2.interpolator,config2.duration,config2.delayTime);
                InterpolatorAnimator alphaAnimation = new InterpolatorAnimator(findViewById(R.id.blackMask), "alpha", 1f, 0f,config6.interpolator,config6.duration,config6.delayTime);
            }
        },50 );

    }
    private void transitionOut(){

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                InterpolatorAnimator topScaleX = new InterpolatorAnimator(findViewById(R.id.timeLabel), "scaleX", 1f, 0.8f,config3.interpolator,config3.duration,config3.delayTime);
                InterpolatorAnimator topScaleY = new InterpolatorAnimator(findViewById(R.id.timeLabel), "scaleY", 1f, 0.8f,config3.interpolator,config3.duration,config3.delayTime);
                InterpolatorAnimator leftTransitionX = new InterpolatorAnimator(findViewById(R.id.leftLabel), "translationX", 0f, -100f,config4.interpolator,config4.duration,config4.delayTime);
                InterpolatorAnimator rightTransitionX = new InterpolatorAnimator(findViewById(R.id.rightLabel), "translationX", 0f, 100f,config4.interpolator,config4.duration,config4.delayTime);
                InterpolatorAnimator middleTransitionY = new InterpolatorAnimator(findViewById(R.id.middleLabel), "translationY", 0f, 100f,config5.interpolator,config5.duration,config5.delayTime);
                InterpolatorAnimator alphaAnimation = new InterpolatorAnimator(findViewById(R.id.blackMask), "alpha", 0f, 1f,config7.interpolator,config7.duration,config7.delayTime);
            }
        },50 );

    }

    //###################### Init 动画初始化 ######################
    private void animationInit(){

        findViewById(R.id.blackMask).setAlpha(1.f);
        findViewById(R.id.timeLabel).setScaleX(0.8f);
        findViewById(R.id.timeLabel).setScaleY(0.8f);
        findViewById(R.id.leftLabel).setTranslationX(-100f);
        findViewById(R.id.rightLabel).setTranslationX(100f);
        findViewById(R.id.middleLabel).setTranslationY(100f);

    }


    // ###################### Init 动画参数设置  ######################
    private void interpolatorConfig(){

        //Find InterpolatorConfigurationView in XML
        mInterpolatorConfiguratorView = (InterpolatorConfigurationView) findViewById(R.id.interpolator_configurator);

        //Setting Interpolator Config
        config0 = InterpolatorConfig.fromInterpolatorDurationDelayTime("QuadEaseOut",200,25);
        config1 = InterpolatorConfig.fromInterpolatorDurationDelayTime("CubicEaseOut",300,50);
        config2 = InterpolatorConfig.fromInterpolatorDurationDelayTime("CubicEaseOut",300,0);
        config3 = InterpolatorConfig.fromInterpolatorDurationDelayTime("QuadEaseOut",250,50);
        config4 = InterpolatorConfig.fromInterpolatorDurationDelayTime("CubicEaseOut",250,0);
        config5 = InterpolatorConfig.fromInterpolatorDurationDelayTime("CubicEaseOut",250,25);
        config6 = InterpolatorConfig.fromInterpolatorDurationDelayTime("CubicEaseOut",300,25);
        config7 = InterpolatorConfig.fromInterpolatorDurationDelayTime("CubicEaseOut",175,25);

        //Add Interpolator Config into ConfigurationView
        InterpolatorConfigRegistry.getInstance().removeAllInterpolatorConfig();
        InterpolatorConfigRegistry.getInstance().addInterpolatorConfig(config0, "时间缩放入场");
        InterpolatorConfigRegistry.getInstance().addInterpolatorConfig(config1, "左右入场");
        InterpolatorConfigRegistry.getInstance().addInterpolatorConfig(config2, "中部入场");
        InterpolatorConfigRegistry.getInstance().addInterpolatorConfig(config3, "时间缩放退场");
        InterpolatorConfigRegistry.getInstance().addInterpolatorConfig(config4, "左右退场");
        InterpolatorConfigRegistry.getInstance().addInterpolatorConfig(config5, "中部退场");
        InterpolatorConfigRegistry.getInstance().addInterpolatorConfig(config6, "蒙版变白");
        InterpolatorConfigRegistry.getInstance().addInterpolatorConfig(config7, "蒙版变黑");

        mInterpolatorConfiguratorView.refreshInterpolatorConfigurations();
        mInterpolatorConfiguratorView.bringToFront();
    }

}

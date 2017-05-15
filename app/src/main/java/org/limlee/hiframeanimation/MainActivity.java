package org.limlee.hiframeanimation;

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

import org.limlee.hiframeanimationlib.FrameAnimationView;
import org.limlee.hiframeanimationlib.FrameDrawable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FRAME_NAME = "trans";

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
    }

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
            mFrameAnimationView.setOneShot(false); //循环播放帧动画
            mFrameAnimationView.addFrameDrawable(frameDrawables); //添加序列帧
            mFrameAnimationView.setOnFrameListener(new FrameAnimationView.OnFrameListener() { //添加监听器
                @Override
                public void onFrameStart() {
                    Log.d(TAG, "帧动画播放开始！");
                }

                @Override
                public void onFrameEnd() {
                    Log.d(TAG, "帧动画播放结束！");
                }
            });
            mFrameAnimationView.start(); //开始播放
            mFrameAnimationView.isControl = true;


        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFrameAnimationView.stop(); //停止播放
        mFrameAnimationView.setOnFrameListener(null); //移除监听器
    }

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

    private float mDistanceProgress = 0;
    private View.OnTouchListener advancedListener;
    private float mStartTouchEventY;

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

                        break;
                    case MotionEvent.ACTION_UP:

                        //unlock判定
                        if(mDistanceProgress>0.6){
                            mSpring.setEndValue(0.8);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    //mImageView.setImageDrawable(getResources().getDrawable(firstSequences[100]));
                                    //mSpring2.setEndValue(1);
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
}

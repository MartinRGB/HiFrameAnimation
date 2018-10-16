package org.martinrgb.hiframeanimation;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrameActivity extends AutoLayoutActivity {
    private static final String TAG = FrameActivity.class.getSimpleName();
    private FrameAnimationView mFrameAnimationView;
    private static final String FRAME_NAME = "trans";
    public boolean isControl = false;
    public boolean isRepeat = true;
    private int FPS = 60;
    private int frameNumber = 293;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deleteBars();
        setContentView(R.layout.activity_frame);
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
                frameDrawable.setMixAlpha(false);
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

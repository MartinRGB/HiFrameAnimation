package org.martinrgb.hiframeanimation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.zhy.autolayout.AutoLayoutActivity;

public class MainActivity extends AutoLayoutActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void transitionToFrame(View v) {
        startActivity(new Intent(this, FrameActivity.class));
    }

    public void transitionToControl(View v) {
        startActivity(new Intent(this, ControlAcitivity.class));
    }
}

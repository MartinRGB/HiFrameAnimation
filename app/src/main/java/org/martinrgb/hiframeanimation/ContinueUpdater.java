package org.martinrgb.hiframeanimation;

import android.os.Handler;
import android.util.Log;

public class ContinueUpdater implements Runnable {

    private Handler repeatUpdateHandler = new Handler();
    private boolean mIsIncrement = false;
    private long REPEAT_DELAY = 16;
    private int CURRENT_VALUE = 0;
    private int MAX_VALUE;
    private int MIN_VALUE;

    UpdateListener updateListener = null;

    public ContinueUpdater(int startVal, int goalVal,UpdateListener listener,long REPEAT_DELAY) {
        setRepeatDelay(REPEAT_DELAY);
        setCurrentValue(startVal);
        if(startVal > goalVal){
            setIncrement(false);
            setMinValue(goalVal);
        }else{
            setIncrement(true);
            setMaxValue(goalVal);
        }
        this.updateListener = listener;
    }

    public ContinueUpdater(int startVal, int goalVal,UpdateListener listener,int FPS) {
        setRepeatDelay((int)Math.floor(1000/FPS));
        setCurrentValue(startVal);
        if(startVal > goalVal){
            setIncrement(false);
            setMinValue(goalVal);
        }else{
            setIncrement(true);
            setMaxValue(goalVal);
        }
        this.updateListener = listener;
    }

    public void run() {
        if(mIsIncrement){
            increment();

        } else {
            decrement();
        }
    }

    public void setIncrement(boolean boo){
        this.mIsIncrement = boo;
    }

    public void setCurrentValue(int val){
        this.CURRENT_VALUE = val;
    }

    public void setMaxValue(int max){
        this.MAX_VALUE = max;
    }

    public void setMinValue(int min){
        this.MIN_VALUE = min;
    }

    public void setRepeatDelay(long delay){
        this.REPEAT_DELAY = delay;
    }


    private void decrement(){
        if(CURRENT_VALUE > MIN_VALUE){
            CURRENT_VALUE--;
            Log.e("decrement",String.valueOf(CURRENT_VALUE));
            updateListener.onUpdate(CURRENT_VALUE);
            ContinueUpdater continueUpdater = new ContinueUpdater(CURRENT_VALUE,MIN_VALUE,updateListener,REPEAT_DELAY);
            repeatUpdateHandler.postDelayed(continueUpdater, REPEAT_DELAY );
        }
        else{
            repeatUpdateHandler.removeCallbacksAndMessages(null);
            updateListener.onEnd();
        }
    }

    private void increment() {
        if (CURRENT_VALUE < MAX_VALUE) {
            CURRENT_VALUE++;
            Log.e("increment", String.valueOf(CURRENT_VALUE));
            updateListener.onUpdate(CURRENT_VALUE);
            ContinueUpdater continueUpdater = new ContinueUpdater(CURRENT_VALUE,MAX_VALUE,updateListener,REPEAT_DELAY);
            repeatUpdateHandler.postDelayed(continueUpdater, REPEAT_DELAY);
        }
        else{
            repeatUpdateHandler.removeCallbacksAndMessages(null);
            updateListener.onEnd();
        }
    }

    public interface UpdateListener {
        public void onUpdate(int frameNumber);
        public void onEnd();
    }
}





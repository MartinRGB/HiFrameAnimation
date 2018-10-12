# HiFrameAnimation

forked from [hidaron/HiFrameAnimation
](https://github.com/hidaron/HiFrameAnimation)

测试了「60FPS 全屏序列帧播放」 | 「手势控制 293 张序列帧」功能


### 1.添加帧动画视图布局

````
 <org.limlee.hiframeanimationlib.FrameAnimationView
        android:id="@+id/frame_animation"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

````

### 2.添加序列帧资源

````
 List<FrameDrawable> frameDrawables = new ArrayList<>();
 for (String framePath : frameList) {
 	FrameDrawable frameDrawable = new FrameDrawable(FRAME_NAME + "/" + framePath, 100);
 	frameDrawables.add(frameDrawable);
 }
 mFrameAnimationView.addFrameDrawable(frameDrawables);

````

### 3.播放帧动画

````
 @Override
 protected void onPostCreate(@Nullable Bundle savedInstanceState) {
 	....
 	mFrameAnimationView.setOneShot(false); //循环播放帧动画
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
 }

````

### 4.停止播放

````
 @Override
 protected void onDestroy() {
 	super.onDestroy();
 	mFrameAnimationView.stop(); //停止播放
 	mFrameAnimationView.setOnFrameListener(null); //移除监听器
 }

````



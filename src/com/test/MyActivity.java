package com.test;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * <pre>
 * 聊天界面布局闪动处理, 基本原理如下：
 *          1. 弹出键盘的时候，会导致 RootView 的bottom 变小，直到容纳 键盘+虚拟按键
 *          2. 收回键盘的时候，会导致 RootView的bottom 变大，直到容纳 虚拟键盘
 *          3. 因为RootView bottom的变化，会导致整个布局高度(bottom - top)的变化，所以就会发生布局闪动的情况. 而为了
 *          避免这种情况，只需要在发生变动的父布局调用 onMeasure() 之前，将子View的高度和配置为最终高度，既可以实现弹
 *          出/收回键盘 不闪动<strong>特定部分布局</strong>的效果(如微信聊天界面)。
 * </pre>
 */
public class MyActivity extends Activity {
    MyLineLayout mll_main;
    FrameLayout fl_list;
    LinearLayout ll_edit;
    EditText et_input;
    Button btn_trigger;
    FrameLayout fl_panel;
    Rect rect = new Rect();

    enum State {
        //空状态
        NONE,
        //打开输入法状态
        KEYBOARD,
        //打开面板状态
        PANEL,
    }

    State state = State.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mll_main = (MyLineLayout) findViewById(R.id.mll_main);
        fl_list = (FrameLayout) findViewById(R.id.fl_list);
        ll_edit = (LinearLayout) findViewById(R.id.ll_edit);
        et_input = (EditText) findViewById(R.id.et_input);
        btn_trigger = (Button) findViewById(R.id.btn_trigger);
        fl_panel = (FrameLayout) findViewById(R.id.fl_panel);
        mll_main.onMeasureListener = new MyLineLayout.OnMeasureListener() {
            /**
             * 可能会发生多次 调用的情况，因为存在 layout_weight 属性，需要2次测试，给定最终大小
             * */
            @Override
            public void onMeasure(int maxHeight, int oldHeight, int nowHeight) {
                switch (state) {
                    case NONE: {

                    }
                    break;
                    case PANEL: {
                        //state 处于 panel 状态只有一种可能，就是主动点击切换到panel，
                        //1.如果之前是keyboard状态，则在本次onMeasure的时候，一定要把panel显示出来
                        //避免 mll 刷动
                        //2. 如果之前处于 none状态，那么本次触发来自于 postDelay，可以忽略
                        fl_panel.setVisibility(View.VISIBLE);
                    }
                    break;
                    case KEYBOARD: {
                        //state = KEYBOARD 状态，只有一种可能，就是主动点击了 EditText
                        //1. 如果之前是panel状态，则一般已经有了固有高度，这个高度刚刚好满足键盘的高度，那么只用隐藏掉
                        //panel 既可以实现页面不进行刷新
                        //2. 如果之前为none状态，则可以忽略
                        fl_panel.setVisibility(View.GONE);
                        //处于键盘状态，需要更新键盘高度为面板的高度
                        if (oldHeight >= nowHeight) {
                            //记录当前的缩放大小为键盘大小
                            int h = maxHeight - nowHeight;
                            //避免 输入法 悬浮状态, 保留一个最低高度
                            if (h < 500) {
                                h = 500;
                            }
                            fl_panel.getLayoutParams().height = h;
                        }
                    }
                    break;
                }
                Log.d("SC_SIZE", String.format("onMeasure %d %d %d", maxHeight, nowHeight, oldHeight));
            }
        };
        fl_list.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftInputView();
                fl_panel.setVisibility(View.GONE);
                state = State.NONE;
                return false;
            }
        });
        et_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state = State.KEYBOARD;
            }
        });
        btn_trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (state) {
                    case NONE:
                    case KEYBOARD: {
                        hideSoftInputView();
                        state = State.PANEL;
                        //无论App 处于什么状态，都追加一个 显示 panel 的方法，避免处于非正常状态无法打开panel
                        getWindow().getDecorView().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                fl_panel.setVisibility(View.VISIBLE);
                            }
                        }, 100);
                    }
                    break;
                    case PANEL: {
                        state = State.NONE;
                        fl_panel.setVisibility(View.GONE);
                    }
                    break;
                }
            }
        });
        //设置基本panel 高度，以使得第一次能正常打开panel
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        fl_panel.getLayoutParams().height = rect.height() / 2;
        fl_panel.setVisibility(View.GONE);
    }

    /**
     * 隐藏软键盘输入
     */
    public void hideSoftInputView() {
        InputMethodManager manager = ((InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE));

        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getCurrentFocus() != null && manager != null)
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * Created by Administrator on 2015/11/20.
     */
    public static class MyLineLayout extends LinearLayout {
        OnMeasureListener onMeasureListener;
        int maxHeight = 0;
        int oldHeight;

        public MyLineLayout(Context context) {
            super(context);
        }

        public MyLineLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = MeasureSpec.getSize(heightMeasureSpec);
            if (onMeasureListener != null) {
                onMeasureListener.onMeasure(maxHeight, oldHeight, height);
            }
            oldHeight = height;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            //之所以，在这里记录 maxHeight的大小，是因为 onMeasure 中可能多次调用，中间可能会逐步出现 ActionBar，BottomVirtualKeyboard，
            //所以 onMeasure中获取的maxHeight存在误差
            if (h > maxHeight) {
                maxHeight = h;
            }
            Log.d("SC_SIZE", String.format("Size Change %d %d", h, oldh));
        }

        interface OnMeasureListener {
            void onMeasure(int maxHeight, int oldHeight, int nowHeight);
        }
    }
}

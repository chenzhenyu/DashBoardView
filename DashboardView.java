package com.actia.monitor.actia_monitor.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.Image;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.actia.monitor.actia_monitor.R;
import com.actia.monitor.actia_monitor.utils.ScreenUtil;

/**
 * 汽车仪表盘控件
 * author:zhenyu chen
 * date:2017/6/23.
 */
public class DashboardView extends ViewGroup {

    private double MAX_ANGLE = 270d;//仪表盘刻度最大角度
    private int DIVIDER_GROUP = 14;//大刻度数量
    private int DIVIDER_CHILD = 2;//单个大刻度下有几个小刻度
    private int WHERE_HIGH = 8;//哪里开始算做高速
    private float LENGTH_BIG_SCALE = ScreenUtil.dpToPx(getContext(), 8);//大刻度长度 dp
    private float LENGTH_SMALL_SCALE = ScreenUtil.dpToPx(getContext(), 5);//小刻度长度 dp
    private float SCALE_BIG_WIDTH = ScreenUtil.dpToPx(getContext(), 3);//大刻度宽度
    private float SCALE_SMALL_WIDTH = ScreenUtil.dpToPx(getContext(), 2);//小刻度宽度
    private float DOT_SIZE = ScreenUtil.dpToPx(getContext(), 14);//点指示器的直径
    private int SCALE_TEXT_SIZE = 10;//刻度字体大小 sp
    private int SCALE_HIGH_COLOR = Color.parseColor("#F70B0B");//刻度字体颜色
    private int SCALE_LOW_COLOR = Color.WHITE;//刻度字体颜色
    private int ARC_COLOR = Color.GRAY;//刻度字体颜色
    private int UNIT_MULTIPLE = 10;//计量单位 是大刻度的倍数
    private float OFFSET_SCALE_TEXT = ScreenUtil.dpToPx(getContext(), 10);//字符偏移大刻度 dp
    private float OFFSET_ARC_TEXT = ScreenUtil.dpToPx(getContext(), 10);//内弧相对刻度标记字符的偏移
    private int DOT_ANIM_INTERIM = 1000;//点（指针）移动动画时间

    private Paint scaleLowSmallPaint;
    private Paint scaleLowBigPaint;
    private Paint scaleHighSmallPaint;
    private Paint scaleHighBigPaint;
    private Paint scaleTextHighPaint;
    private Paint scaleTextLowPaint;
    private Paint arcPaint;

    private int radius;//圆盘半径
    private double angleRes = 0;//保持上一次角度
    private Thread shedule;//动画控制线程

    private ImageView ivDot;

    public DashboardView(Context context) {
        super(context);
        init();
    }

    public DashboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DashboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setPadding(0, 0, 0, 0);
        ivDot = new ImageView(getContext());
        LayoutParams lp = new LayoutParams((int) DOT_SIZE, (int) DOT_SIZE);
        ivDot.setLayoutParams(lp);
        ivDot.setImageResource(R.drawable.ic_dashboard_dot);
        addView(ivDot);
        setDotPos(0);

        scaleLowSmallPaint = new Paint();
        scaleLowSmallPaint.setAntiAlias(true);
        scaleLowSmallPaint.setColor(SCALE_LOW_COLOR);//白色
        scaleLowSmallPaint.setStrokeWidth(SCALE_SMALL_WIDTH);

        scaleLowBigPaint = new Paint();
        scaleLowBigPaint.setAntiAlias(true);
        scaleLowBigPaint.setColor(SCALE_LOW_COLOR);//白色
        scaleLowBigPaint.setStrokeWidth(SCALE_BIG_WIDTH);

        scaleHighSmallPaint = new Paint();
        scaleHighSmallPaint.setAntiAlias(true);
        scaleHighSmallPaint.setColor(SCALE_HIGH_COLOR);//红色
        scaleHighSmallPaint.setStrokeWidth(SCALE_SMALL_WIDTH);

        scaleHighBigPaint = new Paint();
        scaleHighBigPaint.setAntiAlias(true);
        scaleHighBigPaint.setColor(SCALE_HIGH_COLOR);//红色
        scaleHighBigPaint.setStrokeWidth(SCALE_BIG_WIDTH);

        scaleTextLowPaint = new Paint();
        scaleTextLowPaint.setAntiAlias(true);
        scaleTextLowPaint.setColor(SCALE_LOW_COLOR);
        scaleTextLowPaint.setTextSize(ScreenUtil.dpToPx(getContext(), SCALE_TEXT_SIZE));
        scaleTextLowPaint.setTextAlign(Paint.Align.CENTER);

        scaleTextHighPaint = new Paint();
        scaleTextHighPaint.setAntiAlias(true);
        scaleTextHighPaint.setColor(SCALE_HIGH_COLOR);
        scaleTextHighPaint.setTextSize(ScreenUtil.dpToPx(getContext(), SCALE_TEXT_SIZE));
        scaleTextHighPaint.setTextAlign(Paint.Align.CENTER);

        arcPaint = new Paint();
        arcPaint.setAntiAlias(true);
        arcPaint.setStrokeWidth(SCALE_SMALL_WIDTH);
        arcPaint.setColor(ARC_COLOR);
        arcPaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * 获得默认该layout的尺寸
     *
     * @return
     */
    private int getDefaultWidth() {
        WindowManager wm = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return Math.min(outMetrics.widthPixels, outMetrics.heightPixels);
    }

    public void smoothLoadValue(double value) {
        int maxValue = UNIT_MULTIPLE * DIVIDER_GROUP;
        double realValue = 0;
        if (value > maxValue) {
            realValue = maxValue;
        } else {
            if (value < 0) {
                realValue = 0;
            } else {
                realValue = value;
            }
        }
        double curAngle =  (MAX_ANGLE * (realValue / maxValue));
        smoothDotMove(curAngle);
    }

    public void smoothDotMove(double angle) {
        if (angle > MAX_ANGLE) angle = (int) MAX_ANGLE;
        if (shedule != null && shedule.isAlive()) return;
        final int minInterim = (int) ((float) DOT_ANIM_INTERIM / MAX_ANGLE);
        final double finalAngle = angle;
        if (angleRes > angle) {
            shedule = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = (int) angleRes; i >= finalAngle; i--) {
                        Message msg = dotCtrl.obtainMessage();
                        msg.what = WHAT_CHANGE_ANGLE;
                        msg.arg1 = i;
                        dotCtrl.sendMessage(msg);
                        try {
                            Thread.sleep(minInterim);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } else {
            shedule = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = (int) angleRes; i <= finalAngle; i++) {
                        Message msg = dotCtrl.obtainMessage();
                        msg.what = WHAT_CHANGE_ANGLE;
                        msg.arg1 = i;
                        dotCtrl.sendMessage(msg);
                        try {
                            Thread.sleep(minInterim);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        shedule.start();
    }

    private int WHAT_CHANGE_ANGLE;
    private Handler dotCtrl = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == WHAT_CHANGE_ANGLE) {
                setDotPos(msg.arg1);
            }
        }
    };

    private void setDotPos(int angle) {
        if (angle > MAX_ANGLE) angle = (int) MAX_ANGLE;
        angleRes = angle;
        float dotArcRadius = radius - LENGTH_BIG_SCALE - OFFSET_SCALE_TEXT - OFFSET_ARC_TEXT;
        //角度转换
        int alterAngle = angle - 225;
        float x = radius + dotArcRadius * ((float) Math.cos(Math.toRadians(alterAngle)));
        float y = radius + dotArcRadius * ((float) Math.sin(Math.toRadians(alterAngle)));
        int left = (int) (x - DOT_SIZE / 2);
        int top = (int) (y - DOT_SIZE / 2);
        int right = (int) (x + DOT_SIZE / 2);
        int bottom = (int) (y + DOT_SIZE / 2);
        ivDot.layout(left, top, right, bottom);
    }

    public int getWHERE_HIGH() {
        return WHERE_HIGH;
    }

    public void setWHERE_HIGH(int WHERE_HIGH) {
        this.WHERE_HIGH = WHERE_HIGH;
    }

    public double getMAX_ANGLE() {
        return MAX_ANGLE;
    }

    public void setMAX_ANGLE(float MAX_ANGLE) {
        this.MAX_ANGLE = MAX_ANGLE;
    }

    public int getDIVIDER_GROUP() {
        return DIVIDER_GROUP;
    }

    public void setDIVIDER_GROUP(int DIVIDER_GROUP) {
        this.DIVIDER_GROUP = DIVIDER_GROUP;
    }

    public int getDIVIDER_CHILD() {
        return DIVIDER_CHILD;
    }

    public void setDIVIDER_CHILD(int DIVIDER_CHILD) {
        this.DIVIDER_CHILD = DIVIDER_CHILD;
    }

    public int getUNIT_MULTIPLE() {
        return UNIT_MULTIPLE;
    }

    public void setUNIT_MULTIPLE(int UNIT_MULTIPLE) {
        this.UNIT_MULTIPLE = UNIT_MULTIPLE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int resWidth = 0;
        int resHeight = 0;

        /**
         * 根据传入的参数，分别获取测量模式和测量值
         */
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int height = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        /**
         * 如果宽或者高的测量模式非精确值
         */
        if (widthMode != MeasureSpec.EXACTLY
                || heightMode != MeasureSpec.EXACTLY) {
            // 主要设置为背景图的高度
            resWidth = getSuggestedMinimumWidth();
            // 如果未设置背景图片，则设置为屏幕宽高的默认值
            resWidth = resWidth == 0 ? getDefaultWidth() : resWidth;

            resHeight = getSuggestedMinimumHeight();
            // 如果未设置背景图片，则设置为屏幕宽高的默认值
            resHeight = resHeight == 0 ? getDefaultWidth() : resHeight;
        } else {
            // 如果都设置为精确值，则直接取小值；
            resWidth = resHeight = Math.min(width, height);
        }

        setMeasuredDimension(resWidth, resHeight);
        radius = resWidth / 2;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int num_degree = DIVIDER_GROUP * DIVIDER_CHILD;
        double mini_degree_angle = MAX_ANGLE / num_degree;
        //绘制刻度尺
        for (int i = 0; i <= num_degree; i++) {
            double curAngle = -225 + mini_degree_angle * i;
            float x0;
            float y0;
            float x1;
            float y1;
            float x2;
            float y2;
            if (i % DIVIDER_CHILD == 0) {//大刻度
                x0 = (float) (radius + (radius - LENGTH_BIG_SCALE) * Math.cos(Math.toRadians(curAngle)));
                y0 = (float) (radius + (radius - LENGTH_BIG_SCALE) * Math.sin(Math.toRadians(curAngle)));
                x1 = (float) (radius + (radius) * Math.cos(Math.toRadians(curAngle)));
                y1 = (float) (radius + (radius) * Math.sin(Math.toRadians(curAngle)));

                x2 = (float) (radius + (radius - LENGTH_BIG_SCALE - OFFSET_SCALE_TEXT) * Math.cos(Math.toRadians(curAngle)));
                y2 = (float) (radius + (radius - LENGTH_BIG_SCALE - OFFSET_SCALE_TEXT) * Math.sin(Math.toRadians(curAngle)));

                if (i >= WHERE_HIGH * DIVIDER_CHILD) {//高速使用红笔
                    canvas.drawLine(x0, y0, x1, y1, scaleHighBigPaint);
                    canvas.drawText((i / DIVIDER_CHILD) * UNIT_MULTIPLE + "", x2, y2, scaleTextHighPaint);
                } else {
                    canvas.drawLine(x0, y0, x1, y1, scaleLowBigPaint);
                    canvas.drawText((i / DIVIDER_CHILD) * UNIT_MULTIPLE + "", x2, y2, scaleTextLowPaint);
                }
            } else {//小刻度
                x0 = (float) (radius + (radius - LENGTH_SMALL_SCALE) * Math.cos(Math.toRadians(curAngle)));
                y0 = (float) (radius + (radius - LENGTH_SMALL_SCALE) * Math.sin(Math.toRadians(curAngle)));
                x1 = (float) (radius + (radius) * Math.cos(Math.toRadians(curAngle)));
                y1 = (float) (radius + (radius) * Math.sin(Math.toRadians(curAngle)));

                if (i >= WHERE_HIGH * DIVIDER_CHILD) {//高速使用红笔
                    canvas.drawLine(x0, y0, x1, y1, scaleHighSmallPaint);
                } else {
                    canvas.drawLine(x0, y0, x1, y1, scaleLowSmallPaint);
                }
            }
        }

        //绘制弧
        float topOrLeft = LENGTH_BIG_SCALE + OFFSET_SCALE_TEXT + OFFSET_ARC_TEXT;
        float bottomOrRight = (float) 2 * radius - LENGTH_BIG_SCALE - OFFSET_SCALE_TEXT - OFFSET_ARC_TEXT;
        canvas.drawArc(new RectF(topOrLeft, topOrLeft, bottomOrRight, bottomOrRight), -225, 270, false, arcPaint);
    }
}

package com.cod3rboy.routinetask.views;


import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.cod3rboy.routinetask.R;

import java.util.Locale;

/**
 * @Todo Implement Countdown timer
 */

public class ClockSeekView extends CircularSeekBar {

    protected float mDialCircleRadius;
    protected float mAnimCircleStartRadius;
    protected float mAnimCircleEndRadius;
    protected float mAnimCircleRadius;
    protected float mDialRingRadius;
    protected float mMinTextSize;
    protected float mSecTextSize;
    protected float mLblTextSize;

    protected float mDialRingLinesWidth;
    protected float mDialRingLinesStrokeWidth;
    protected float mDialRingStrokeWidth;

    private Paint mDialCirclePaint;
    private Paint mAnimCirclePaint;
    private Paint mDialRingPaint;
    private Paint mDialRingLinesPaint;
    private Paint mMinTextPaint;
    private Paint mSecTextPaint;
    private Paint mLblTextPaint;

    private int mDialMinutes;
    private int mDialSeconds;

    private ValueAnimator mRadiusAnimator;

    public ClockSeekView(Context context) {
        super(context);
        init(null);
    }

    public ClockSeekView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(getContext().obtainStyledAttributes(attrs, R.styleable.ClockSeekView, 0,0));
    }

    public ClockSeekView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(getContext().obtainStyledAttributes(attrs, R.styleable.ClockSeekView, 0,0));
    }

    private void init(TypedArray array){

        mDialCircleRadius = array.getDimension(R.styleable.ClockSeekView_dial_circle_radius, 80) * DPTOPX_SCALE;

        mAnimCircleStartRadius = array.getDimension(R.styleable.ClockSeekView_anim_circle_start_radius, 80) * DPTOPX_SCALE;
        mAnimCircleEndRadius = array.getDimension(R.styleable.ClockSeekView_anim_circle_end_radius, 100) * DPTOPX_SCALE;
        mAnimCircleRadius = 0;

        mDialRingRadius = array.getDimension(R.styleable.ClockSeekView_ring_radius, 90) * DPTOPX_SCALE;

        mDialRingLinesWidth = array.getDimension(R.styleable.ClockSeekView_ring_lines_width, 1) * DPTOPX_SCALE;
        mDialRingLinesStrokeWidth = array.getDimension(R.styleable.ClockSeekView_ring_lines_stroke_width, 2) * DPTOPX_SCALE;
        mDialRingStrokeWidth = array.getDimension(R.styleable.ClockSeekView_ring_stroke_width, 1) * DPTOPX_SCALE;

        mDialMinutes = 0;
        mDialSeconds = 0;

        mMinTextSize = array.getDimension(R.styleable.ClockSeekView_min_text_size, 40) * DPTOPX_SCALE;
        mSecTextSize = array.getDimension(R.styleable.ClockSeekView_sec_text_size, 20) * DPTOPX_SCALE;
        mLblTextSize = array.getDimension(R.styleable.ClockSeekView_lbl_text_size, 12) * DPTOPX_SCALE;

        // Initialize Paint Objects
        mDialCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDialCirclePaint.setStyle(Paint.Style.FILL);
        mDialCirclePaint.setColor(array.getColor(R.styleable.ClockSeekView_dial_circle_color, 0xFFDDDDDD));

        mAnimCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAnimCirclePaint.setStyle(Paint.Style.FILL);
        mAnimCirclePaint.setColor(array.getColor(R.styleable.ClockSeekView_anim_circle_color, 0xFFEEEEEE));

        mDialRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDialRingPaint.setStyle(Paint.Style.STROKE);
        mDialRingPaint.setStrokeWidth(mDialRingStrokeWidth);
        mDialRingPaint.setColor(array.getColor(R.styleable.ClockSeekView_ring_color,0xFF003DA4));

        mDialRingLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDialRingLinesPaint.setStyle(Paint.Style.STROKE);
        mDialRingLinesPaint.setStrokeWidth(mDialRingLinesStrokeWidth);
        mDialRingLinesPaint.setColor(array.getColor(R.styleable.ClockSeekView_ring_lines_color, 0x66003DA4));

        mMinTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMinTextPaint.setTextAlign(Paint.Align.CENTER);
        mMinTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mMinTextPaint.setColor(array.getColor(R.styleable.ClockSeekView_min_text_color, 0xFF000000));
        mMinTextPaint.setTextSize(mMinTextSize);
        mMinTextPaint.setLetterSpacing(0.15f);

        mSecTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecTextPaint.setTextAlign(Paint.Align.CENTER);
        mSecTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mSecTextPaint.setColor(array.getColor(R.styleable.ClockSeekView_sec_text_color, 0xFF000000));
        mSecTextPaint.setTextSize(mSecTextSize);
        mSecTextPaint.setLetterSpacing(0.15f);

        mLblTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLblTextPaint.setTextAlign(Paint.Align.CENTER);
        mLblTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mLblTextPaint.setColor(array.getColor(R.styleable.ClockSeekView_lbl_text_color, 0xFF000000));
        mLblTextPaint.setTextSize(mLblTextSize);
        mLblTextPaint.setLetterSpacing(0.15f);

        mRadiusAnimator = ValueAnimator.ofFloat();
        mRadiusAnimator.setDuration(600);
        mRadiusAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mRadiusAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mRadiusAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mRadiusAnimator.addUpdateListener((ValueAnimator valueAnimator)->{
            mAnimCircleRadius = (Float)valueAnimator.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.translate(this.getWidth() / 2, this.getHeight() / 2);
        if(mRadiusAnimator.isRunning()) {
            // Draw Animation Circle
            canvas.drawCircle(0, 0, mAnimCircleRadius, mAnimCirclePaint);
        }
        // Draw Dial Fill Circle
        canvas.drawCircle(0, 0, mDialCircleRadius, mDialCirclePaint);
        // Draw Dial Ring Circle
        canvas.drawCircle(0,0, mDialRingRadius, mDialRingPaint);
        // Draw Dial Ring Lines
        for(int angle = 0; angle < 360; angle += 6){
            // Make lines bold at 90 deg interval
            if(angle % 90 == 0) {
                mDialRingLinesPaint.setStrokeWidth(mDialRingLinesStrokeWidth * 2);
            }else{
                mDialRingLinesPaint.setStrokeWidth(mDialRingLinesStrokeWidth);
            }
            // Calculate first point
            float x1 = (float) ((mDialRingRadius - mDialRingLinesWidth) * Math.cos(angle * 2*Math.PI / 360f));
            float y1 = (float) ((mDialRingRadius - mDialRingLinesWidth) * Math.sin(angle * 2*Math.PI / 360f));
            // Calculate second points
            float x2 = (float) ((mDialRingRadius) * Math.cos(angle * 2*Math.PI / 360f));
            float y2 = (float) ((mDialRingRadius) * Math.sin(angle * 2*Math.PI / 360f));
            // Draw line joining two points
            canvas.drawLine(x1,y1,x2,y2, mDialRingLinesPaint);
        }


        // Draw Dial Minutes Text
        float yPosMinText = (-(mMinTextPaint.ascent() + mMinTextPaint.descent())/2f) - mDialCircleRadius * 0.5f;
        canvas.drawText(String.format(Locale.getDefault(),"%02d", mDialMinutes),0, yPosMinText, mMinTextPaint);

        // Draw Minutes Label
        float yPosMinLabel = -(mLblTextPaint.ascent() + mLblTextPaint.descent())/2f;
        canvas.drawText("Mins", 0, yPosMinLabel, mLblTextPaint);

        // Draw Seconds Text
        float yPosSecText = (-(mSecTextPaint.ascent() + mSecTextPaint.descent())/2f) + mDialCircleRadius * 0.4f;
        canvas.drawText(String.format(Locale.getDefault(),"%02d", mDialSeconds), 0, yPosSecText, mSecTextPaint);

        // Draw Seconds Label
        float yPosSecLabel =  (-(mLblTextPaint.ascent() + mLblTextPaint.descent())/2f) + mDialCircleRadius * 0.7f;
        canvas.drawText("Secs", 0, yPosSecLabel, mLblTextPaint);


        super.onDraw(canvas); // Draw Circular SeekBar View
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if(result){
            // Update minutes on Dial
            mDialMinutes = (int) Math.floor(mProgress/100f * 60f);
        }
        return result;
    }

    public int getDialTime(){
        return mDialMinutes * 60; // Return in seconds
    }
    public void setDialTime(int seconds){
        mDialMinutes = seconds / 60;
        mDialSeconds = seconds % 60;
        int progress = (int) Math.ceil(mDialMinutes/60.0 * 100);
        setProgress(progress);
        invalidate();
    }

    public void startDialAnimation(){
        //Log.d("ClockSeekView", "Starting animation. Start Radius = " + mDialCircleAnimRadius + " and Stop Radius = " + (getMeasuredHeight() * 0.5f));
        mRadiusAnimator.setFloatValues(mAnimCircleStartRadius, mAnimCircleEndRadius);
        mRadiusAnimator.start();
    }
    public void stopDialAnimation(){
        mRadiusAnimator.end();
        invalidate();
    }
    public void setTouchEnabled(boolean isEnabled){
        isTouchEnabled = isEnabled;
    }
}

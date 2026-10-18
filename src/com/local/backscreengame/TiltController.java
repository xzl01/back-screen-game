package com.local.backscreengame;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/** 加速度计倾斜控制：启动后先采基准重力，再按相对倾斜方向发方向事件。 */
public final class TiltController implements SensorEventListener {
    private static final int CALIB_SAMPLES = 40;
    private static final float ALPHA = 0.2f;
    private static final float DEADZONE = 1.5f;

    public interface Listener {
        void onDirection(int dx, int dy);
    }

    private final SensorManager sm;
    private final Sensor sensor;
    private final Listener listener;
    private float fx, fy;
    private float baseX, baseY;
    private int calib;
    private boolean invertX, invertY;
    private int lastDx, lastDy;
    public String status = "待校准";

    public TiltController(Context context, Listener listener) {
        this.listener = listener;
        sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public boolean hasSensor() {
        return sensor != null;
    }

    public void setInvert(boolean x, boolean y) {
        invertX = x;
        invertY = y;
    }

    public boolean start() {
        if (sensor == null) {
            status = "没有加速度计";
            return false;
        }
        calib = 0;
        fx = fy = baseX = baseY = 0;
        lastDx = lastDy = 0;
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        status = "校准中（保持手机稳定）";
        return true;
    }

    public void stop() {
        if (sm != null) sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float x = e.values[0];
        float y = e.values[1];
        fx = ALPHA * x + (1 - ALPHA) * fx;
        fy = ALPHA * y + (1 - ALPHA) * fy;
        if (calib < CALIB_SAMPLES) {
            baseX += fx;
            baseY += fy;
            calib++;
            if (calib == CALIB_SAMPLES) status = "校准完成";
            return;
        }
        float bx = baseX / CALIB_SAMPLES;
        float by = baseY / CALIB_SAMPLES;
        float ddx = fx - bx;
        float ddy = fy - by;
        if (invertX) ddx = -ddx;
        if (invertY) ddy = -ddy;
        int mx = 0;
        int my = 0;
        if (Math.abs(ddx) > Math.abs(ddy)) {
            if (Math.abs(ddx) > DEADZONE) mx = ddx > 0 ? 1 : -1;
        } else {
            if (Math.abs(ddy) > DEADZONE) my = ddy > 0 ? 1 : -1;
        }
        if (mx != lastDx || my != lastDy) {
            lastDx = mx;
            lastDy = my;
            // 包括 (0,0)：手机回正时通知游戏停下，避免球一直自己滚/挡板停不下来
            listener.onDirection(mx, my);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}

package com.local.backscreengame;

/** 所有背屏小游戏的统一接口。 */
public interface GameView {
    String name();

    /** 游戏提示（正面屏幕显示）。 */
    String hint();

    void reset();

    /** 倾斜输入：dx/dy 为 -1/0/1。 */
    void onTilt(int dx, int dy);

    /** 动作键（音量键/AI 键/正面按钮）。 */
    void onAction();

    /** 推进一帧逻辑。 */
    void tick();

    /** 渲染到 391 格亮度帧（row*17+col）。 */
    void render(int[] frame);

    boolean isAlive();

    boolean isWon();

    int tickMs();

    String status();
}

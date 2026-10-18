package com.local.backscreengame;

/** 17×23 点阵乒乓：倾斜上下控制左侧挡板，先到 5 分胜。 */
public final class PongGame implements GameView {
    public static final int COLS = 17;
    public static final int ROWS = 23;
    public static final int CELLS = COLS * ROWS;

    private static final int PADDLE_H = 4;
    private static final int WIN_SCORE = 5;
    private static final int LEFT_X = 1;
    private static final int RIGHT_X = COLS - 2;

    private float leftY, rightY;
    private float ballX, ballY, vx, vy;
    private int lastTilt;
    private int userScore, aiScore;
    private int serveDelay = 30;
    private boolean alive = true;
    private boolean won = false;
    private int tickMs = 70;

    public PongGame() {
        reset();
    }

    @Override
    public String name() {
        return "乒乓";
    }

    @Override
    public String hint() {
        return "倾斜上下挡板 · 先到 5 分获胜";
    }

    @Override
    public void reset() {
        leftY = (ROWS - PADDLE_H) / 2f;
        rightY = (ROWS - PADDLE_H) / 2f;
        userScore = 0;
        aiScore = 0;
        alive = true;
        won = false;
        serveDelay = 30;
        lastTilt = 0;
        centerBall(1);
    }

    private void centerBall(int dir) {
        ballX = COLS / 2f;
        ballY = ROWS / 2f;
        vx = 0.6f * dir;
        vy = 0.35f;
    }

    @Override
    public void onTilt(int dx, int dy) {
        lastTilt = Math.max(-1, Math.min(1, dy));
    }

    @Override
    public void onAction() {
        if (alive && serveDelay > 0) serveDelay = 0;
    }

    @Override
    public void tick() {
        if (!alive) return;
        if (lastTilt != 0) {
            leftY = Math.max(0, Math.min(ROWS - PADDLE_H, leftY + lastTilt));
        }
        if (serveDelay > 0) {
            serveDelay--;
            if (serveDelay == 0) centerBall(Math.random() < 0.5 ? 1 : -1);
            return;
        }

        ballX += vx;
        ballY += vy;
        if (ballY < 0.5f) {
            ballY = 0.5f;
            vy = Math.abs(vy);
        } else if (ballY > ROWS - 1.5f) {
            ballY = ROWS - 1.5f;
            vy = -Math.abs(vy);
        }

        // 左挡板（玩家）
        if (vx < 0 && ballX <= LEFT_X + 0.9f && ballY >= leftY - 0.6f && ballY <= leftY + PADDLE_H + 0.6f) {
            ballX = LEFT_X + 0.9f;
            float off = (ballY - (leftY + PADDLE_H / 2f)) * 0.12f;
            vy = Math.max(-1.0f, Math.min(1.0f, vy * 0.8f + off));
            vx = Math.abs(vx) * 1.04f;
        }
        // 右挡板（AI）
        if (vx > 0 && ballX >= RIGHT_X - 0.9f && ballY >= rightY - 0.6f && ballY <= rightY + PADDLE_H + 0.6f) {
            ballX = RIGHT_X - 0.9f;
            float off = (ballY - (rightY + PADDLE_H / 2f)) * 0.12f;
            vy = Math.max(-1.0f, Math.min(1.0f, vy * 0.8f + off));
            vx = -Math.abs(vx) * 1.04f;
        }

        // AI 跟随
        float target = ballY - PADDLE_H / 2f;
        if (rightY < target) rightY = Math.min(ROWS - PADDLE_H, rightY + 0.5f);
        else if (rightY > target) rightY = Math.max(0, rightY - 0.5f);

        // 得分
        if (ballX < -1f) {
            aiScore++;
            checkWin();
            serveDelay = 35;
            centerBall(1);
        } else if (ballX > COLS + 1f) {
            userScore++;
            checkWin();
            serveDelay = 35;
            centerBall(-1);
        }
    }

    private void checkWin() {
        if (userScore >= WIN_SCORE) {
            alive = false;
            won = true;
        } else if (aiScore >= WIN_SCORE) {
            alive = false;
            won = false;
        }
    }

    @Override
    public void render(int[] frame) {
        for (int i = 0; i < CELLS; i++) frame[i] = 0;
        if (!alive) {
            if (won) {
                for (int i = 0; i < CELLS; i++) frame[i] = 200;
            } else {
                for (int r = 0; r < ROWS; r++) {
                    frame[r * COLS + Math.min(r, COLS - 1)] = 255;
                    frame[r * COLS + Math.max(0, COLS - 1 - r)] = 255;
                }
            }
            return;
        }
        for (int i = 0; i < PADDLE_H; i++) frame[(Math.round(leftY) + i) * COLS + LEFT_X] = 220;
        for (int i = 0; i < PADDLE_H; i++) frame[(Math.round(rightY) + i) * COLS + RIGHT_X] = 220;
        for (int y = 0; y < ROWS; y += 2) frame[y * COLS + COLS / 2] = 24;   // 中线
        if (serveDelay <= 0) {
            int bx = Math.round(ballX);
            int by = Math.round(ballY);
            if (bx >= 0 && bx < COLS && by >= 0 && by < ROWS) frame[by * COLS + bx] = 255;
        }
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public boolean isWon() {
        return won;
    }

    @Override
    public int tickMs() {
        return tickMs;
    }

    @Override
    public String status() {
        return "你 " + userScore + " : " + aiScore + " 电脑（先到 " + WIN_SCORE + " 分）";
    }
}

package com.local.backscreengame;

/** 17×23 点阵打砖块：倾斜左右控制挡板，动作键发球。 */
public final class BreakoutGame implements GameView {
    public static final int COLS = 17;
    public static final int ROWS = 23;
    public static final int CELLS = COLS * ROWS;

    private static final int PADDLE_Y = 21;
    private static final int PADDLE_W = 4;
    private static final int PADDLE_MAX_X = COLS - PADDLE_W;
    private static final int BRICK_ROWS = 4;
    private static final int BRICK_COLS = 15;

    private final boolean[][] bricks = new boolean[BRICK_ROWS][BRICK_COLS];
    private float ballX, ballY, vx, vy;
    private int paddleX = 6;
    private int lastTilt;
    private boolean launched;
    private int score, lives;
    private boolean alive = true;
    private boolean won = false;
    private int tickMs = 70;

    public BreakoutGame() {
        reset();
    }

    @Override
    public String name() {
        return "打砖块";
    }

    @Override
    public String hint() {
        return "倾斜左右挡板 · 按音量键发球（不会自动发球）";
    }

    @Override
    public void reset() {
        for (int r = 0; r < BRICK_ROWS; r++) {
            for (int c = 0; c < BRICK_COLS; c++) bricks[r][c] = true;
        }
        paddleX = 6;
        lastTilt = 0;
        score = 0;
        lives = 3;
        alive = true;
        won = false;
        respawnBall();
    }

    private void respawnBall() {
        launched = false;
        ballX = paddleX + PADDLE_W / 2f;
        ballY = PADDLE_Y - 1.2f;
        vx = 0.35f;
        vy = -0.75f;
    }

    @Override
    public void onTilt(int dx, int dy) {
        lastTilt = Math.max(-1, Math.min(1, dx));
    }

    @Override
    public void onAction() {
        if (alive && !launched) {
            launched = true;
        }
    }

    @Override
    public void tick() {
        if (!alive) return;
        if (lastTilt != 0) {
            paddleX = Math.max(0, Math.min(PADDLE_MAX_X, paddleX + lastTilt));
        }
        if (!launched) {
            ballX = paddleX + PADDLE_W / 2f;
            ballY = PADDLE_Y - 1.2f;
            return;   // 必须按音量键/动作键发球，不自动发球
        }

        ballX += vx;
        ballY += vy;

        if (ballX < 0.5f) {
            ballX = 0.5f;
            vx = Math.abs(vx);
        } else if (ballX > COLS - 0.5f) {
            ballX = COLS - 0.5f;
            vx = -Math.abs(vx);
        }
        if (ballY < 0.5f) {
            ballY = 0.5f;
            vy = Math.abs(vy);
        }

        // 挡板
        if (vy > 0 && ballY >= PADDLE_Y - 0.8f && ballY <= PADDLE_Y + 1.2f
                && ballX >= paddleX - 0.4f && ballX <= paddleX + PADDLE_W + 0.4f) {
            ballY = PADDLE_Y - 0.9f;
            float off = (ballX - (paddleX + PADDLE_W / 2f)) * 0.16f;
            vx = Math.max(-1.1f, Math.min(1.1f, vx * 0.8f + off));
            vy = -Math.abs(vy);
        }

        // 掉出屏幕
        if (ballY > ROWS - 1.2f) {
            lives--;
            if (lives <= 0) {
                alive = false;
                return;
            }
            respawnBall();
            return;
        }

        // 砖块
        int bx = Math.round(ballX);
        int by = Math.round(ballY);
        if (by >= 1 && by <= BRICK_ROWS && bx >= 1 && bx <= BRICK_COLS) {
            if (bricks[by - 1][bx - 1]) {
                bricks[by - 1][bx - 1] = false;
                score++;
                vy = -vy;
                if (remainingBricks() == 0) {
                    won = true;
                    alive = false;
                }
            }
        }
    }

    private int remainingBricks() {
        int n = 0;
        for (int r = 0; r < BRICK_ROWS; r++) {
            for (int c = 0; c < BRICK_COLS; c++) if (bricks[r][c]) n++;
        }
        return n;
    }

    @Override
    public void render(int[] frame) {
        for (int i = 0; i < CELLS; i++) frame[i] = 0;
        if (won) {
            for (int i = 0; i < CELLS; i++) frame[i] = 200;   // 通关：全亮
            return;
        }
        if (!alive) {
            for (int r = 0; r < ROWS; r++) {
                frame[r * COLS + Math.min(r, COLS - 1)] = 255;
                frame[r * COLS + Math.max(0, COLS - 1 - r)] = 255;
            }
            return;
        }
        for (int r = 0; r < BRICK_ROWS; r++) {
            for (int c = 0; c < BRICK_COLS; c++) {
                if (bricks[r][c]) frame[(r + 1) * COLS + (c + 1)] = 170;
            }
        }
        for (int i = 0; i < PADDLE_W; i++) frame[PADDLE_Y * COLS + paddleX + i] = 220;
        int bx = Math.round(ballX);
        int by = Math.round(ballY);
        if (bx >= 0 && bx < COLS && by >= 0 && by < ROWS) frame[by * COLS + bx] = 255;
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
        return "得分：" + score + "　生命：" + Math.max(0, lives);
    }
}

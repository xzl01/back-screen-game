package com.local.backscreengame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;


/** 17×23 点阵 Flappy Bird：按音量键/动作键拍翅膀穿过缝隙。 */
public final class FlappyGame implements GameView {
    public static final int COLS = 17;
    public static final int ROWS = 23;
    public static final int CELLS = COLS * ROWS;

    private static final int BIRD_X = 4;
    private static final int GAP = 7;
    private static final float GRAVITY = 0.18f;
    private static final float FLAP = -1.2f;

    /** {x, gapTop, scored} */
    private final ArrayList<int[]> pipes = new ArrayList<>();
    private final Random rnd = new Random();

    private float birdY;
    private float vel;
    private int score;
    private boolean alive = true;
    private boolean started;
    private int tick;
    private int tickMs = 70;

    public FlappyGame() {
        reset();
    }

    @Override
    public String name() {
        return "Flappy Bird";
    }

    @Override
    public String hint() {
        return "按音量键拍翅膀 · 穿过管道缝隙";
    }

    @Override
    public void reset() {
        birdY = ROWS / 2f;
        vel = 0;
        score = 0;
        alive = true;
        started = false;
        tick = 0;
        pipes.clear();
        pipes.add(new int[]{13, 5 + rnd.nextInt(ROWS - GAP - 7), 0});
    }

    @Override
    public void onTilt(int dx, int dy) {
    }

    @Override
    public void onAction() {
        if (!alive) return;
        started = true;
        vel = FLAP;
    }

    @Override
    public void tick() {
        if (!alive) return;
        if (!started) return;   // 待机：等玩家拍第一下再开始下落
        tick++;
        vel += GRAVITY;
        birdY += vel;
        if (birdY < 0.5f || birdY > ROWS - 1.5f) {
            alive = false;
            return;
        }
        if (tick % 5 == 0) {
            for (int[] p : pipes) p[0] -= 1;
        }
        int lastX = pipes.isEmpty() ? 99 : pipes.get(pipes.size() - 1)[0];
        if (lastX <= COLS - 7) {
            pipes.add(new int[]{COLS - 1, 2 + rnd.nextInt(ROWS - GAP - 4), 0});
        }
        Iterator<int[]> it = pipes.iterator();
        while (it.hasNext()) {
            int[] p = it.next();
            if (p[0] < -1) {
                it.remove();
                continue;
            }
            if (p[0] < BIRD_X && p[2] == 0) {
                p[2] = 1;
                score++;
            }
        }
        int by = Math.round(birdY);
        for (int[] p : pipes) {
            if (p[0] == BIRD_X && (by < p[1] || by > p[1] + GAP - 1)) {
                alive = false;
                return;
            }
        }
    }

    @Override
    public void render(int[] frame) {
        for (int i = 0; i < CELLS; i++) frame[i] = 0;
        if (!alive) {
            for (int r = 0; r < ROWS; r++) {
                frame[r * COLS + Math.min(r, COLS - 1)] = 255;
                frame[r * COLS + Math.max(0, COLS - 1 - r)] = 255;
            }
            return;
        }
        for (int[] p : pipes) {
            if (p[0] < 0 || p[0] >= COLS) continue;
            for (int y = 0; y < ROWS; y++) {
                if (y < p[1] || y > p[1] + GAP - 1) {
                    frame[y * COLS + p[0]] = 170;
                }
            }
        }
        int by = Math.round(birdY);
        int bx = BIRD_X;
        if (by >= 0 && by < ROWS) frame[by * COLS + bx] = 255;
        if (by + 1 < ROWS) frame[(by + 1) * COLS + bx] = 140;
    }

    @Override
    public boolean isAlive() {
        return alive;
    }

    @Override
    public boolean isWon() {
        return false;
    }

    @Override
    public int tickMs() {
        return tickMs;
    }

    @Override
    public String status() {
        if (!started) return "按音量键拍翅开始";
        return "得分：" + score;
    }
}

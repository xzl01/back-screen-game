package com.local.backscreengame;

import java.util.ArrayDeque;
import java.util.Random;

/** 17×23 点阵贪吃蛇：穿墙（环形边界）。 */
public final class SnakeGame implements GameView {
    public static final int COLS = 17;
    public static final int ROWS = 23;
    public static final int CELLS = COLS * ROWS;

    private final ArrayDeque<Integer> body = new ArrayDeque<>();
    private final Random rnd = new Random();
    private int dx = 1, dy = 0, ndx = 1, ndy = 0;
    private int food = -1;
    private int score = 0;
    private boolean alive = true;
    private int tickMs = 200;
    private long tick = 0;

    public SnakeGame() {
        reset();
    }

    @Override
    public String name() {
        return "贪吃蛇";
    }

    @Override
    public String hint() {
        return "倾斜控制方向 · 蛇会穿墙（右边出去左边回来）";
    }

    @Override
    public void reset() {
        body.clear();
        int cx = COLS / 2;
        int cy = ROWS / 2;
        for (int i = 0; i < 3; i++) body.addLast((cy * COLS) + (cx - i));
        dx = 1;
        dy = 0;
        ndx = 1;
        ndy = 0;
        score = 0;
        alive = true;
        tickMs = 200;
        tick = 0;
        food = -1;
        spawnFood();
    }

    @Override
    public void onTilt(int x, int y) {
        if (x == 0 && y == 0) return;
        if (x == -dx && y == -dy) return;
        ndx = x;
        ndy = y;
    }

    @Override
    public void onAction() {
    }

    @Override
    public void tick() {
        if (!alive) return;
        tick++;
        dx = ndx;
        dy = ndy;
        int hx = body.peekFirst() % COLS;
        int hy = body.peekFirst() / COLS;
        int nx = (hx + dx + COLS) % COLS;
        int ny = (hy + dy + ROWS) % ROWS;
        int ni = ny * COLS + nx;

        boolean eating = (ni == food);
        int tail = body.peekLast();
        for (int cell : body) {
            if (cell == ni && (eating || cell != tail)) {
                alive = false;
                return;
            }
        }
        body.addFirst(ni);
        if (eating) {
            score++;
            tickMs = Math.max(85, tickMs - 5);
            spawnFood();
        } else {
            body.removeLast();
        }
    }

    private void spawnFood() {
        if (body.size() >= CELLS) {
            food = -1;
            return;
        }
        int[] free = new int[CELLS];
        int n = 0;
        for (int i = 0; i < CELLS; i++) if (!body.contains(i)) free[n++] = i;
        food = free[rnd.nextInt(n)];
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
        if (food >= 0) frame[food] = (tick % 2 == 0) ? 255 : 170;
        int i = 0;
        int n = body.size();
        for (int cell : body) {
            frame[cell] = (i == 0) ? 255 : (i == n - 1 ? 90 : 150);
            i++;
        }
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
        return "分数：" + score + "　长度：" + body.size();
    }
}

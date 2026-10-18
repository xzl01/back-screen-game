package com.local.backscreengame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/** 17×23 点阵竖版打飞机：倾斜左右走位，音量键/动作键射击。 */
public final class ShooterGame implements GameView {
    public static final int COLS = 17;
    public static final int ROWS = 23;
    public static final int CELLS = COLS * ROWS;

    private static final int SHIP_ROW = 21;

    private final ArrayList<int[]> bullets = new ArrayList<>();   // {x, y}
    private final ArrayList<int[]> enemies = new ArrayList<>();   // {x, y}
    private final Random rnd = new Random();

    private int shipX;
    private int lastTilt;
    private int score;
    private int lives;
    private boolean alive = true;
    private boolean shootRequested;
    private int shootCooldown;
    private int spawnTimer;
    private int tick;
    private int tickMs = 60;

    public ShooterGame() {
        reset();
    }

    @Override
    public String name() {
        return "打飞机";
    }

    @Override
    public String hint() {
        return "倾斜左右走位 · 音量键射击（击落敌机得分）";
    }

    @Override
    public void reset() {
        shipX = COLS / 2;
        lastTilt = 0;
        bullets.clear();
        enemies.clear();
        score = 0;
        lives = 3;
        alive = true;
        shootRequested = false;
        shootCooldown = 0;
        spawnTimer = 16;
        tick = 0;
    }

    @Override
    public void onTilt(int dx, int dy) {
        lastTilt = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
    }

    @Override
    public void onAction() {
        shootRequested = true;
    }

    @Override
    public void tick() {
        if (!alive) return;
        tick++;

        if (lastTilt != 0) shipX = Math.max(1, Math.min(COLS - 2, shipX + lastTilt));
        if (shootCooldown > 0) shootCooldown--;
        if (shootRequested && shootCooldown == 0) {
            bullets.add(new int[]{shipX, SHIP_ROW - 1});
            shootCooldown = 6;
            shootRequested = false;
        }

        // 子弹上移
        Iterator<int[]> bi = bullets.iterator();
        while (bi.hasNext()) {
            int[] b = bi.next();
            b[1] -= 1;
            if (b[1] < 0) bi.remove();
        }

        // 敌机
        spawnTimer--;
        if (spawnTimer <= 0) {
            enemies.add(new int[]{1 + rnd.nextInt(COLS - 2), 0});
            spawnTimer = Math.max(12, 30 - score / 3);
        }
        if (tick % 4 == 0) {
            for (int[] e : enemies) e[1] += 1;
        }

        // 子弹击中敌机
        Iterator<int[]> bi2 = bullets.iterator();
        while (bi2.hasNext()) {
            int[] b = bi2.next();
            boolean hit = false;
            Iterator<int[]> ei = enemies.iterator();
            while (ei.hasNext()) {
                int[] e = ei.next();
                if (Math.abs(b[0] - e[0]) <= 0 && Math.abs(b[1] - e[1]) <= 1) {
                    ei.remove();
                    hit = true;
                    break;
                }
            }
            if (hit) {
                bi2.remove();
                score++;
            }
        }

        // 敌机撞机/漏过
        Iterator<int[]> ei2 = enemies.iterator();
        while (ei2.hasNext()) {
            int[] e = ei2.next();
            boolean crash = (e[1] >= SHIP_ROW - 1 && Math.abs(e[0] - shipX) <= 1) || e[1] >= ROWS - 1;
            if (crash) {
                ei2.remove();
                lives--;
                if (lives <= 0) alive = false;
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
        for (int[] b : bullets) {
            if (b[0] >= 0 && b[0] < COLS && b[1] >= 0 && b[1] < ROWS) {
                frame[b[1] * COLS + b[0]] = 255;
            }
        }
        for (int[] e : enemies) {
            if (e[0] >= 0 && e[0] < COLS && e[1] >= 0 && e[1] < ROWS) {
                frame[e[1] * COLS + e[0]] = 200;
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            int x = shipX + dx;
            if (x >= 0 && x < COLS) frame[SHIP_ROW * COLS + x] = 255;
        }
        frame[(SHIP_ROW + 1) * COLS + shipX] = 180;
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
        return "得分：" + score + "　生命：" + Math.max(0, lives);
    }
}

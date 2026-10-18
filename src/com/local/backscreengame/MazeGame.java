package com.local.backscreengame;

/** 17×23 点阵迷宫滚球：多关卡（关号越高宝珠越多），支持 Amoo 生成的 ASCII 自定义关卡。 */
public final class MazeGame implements GameView {
    public static final int COLS = MazeLevel.COLS;
    public static final int ROWS = MazeLevel.ROWS;
    public static final int CELLS = MazeLevel.CELLS;

    private final boolean[] walls = new boolean[CELLS];
    private final boolean[] orbs = new boolean[CELLS];

    private MazeLevel level;
    private int levelNumber;
    private boolean custom;

    private int ballX, ballY;
    private int exitX, exitY;
    private int lastDx, lastDy;
    private int moveCooldown;
    private int collected, totalOrbs;
    private boolean alive = true;
    private boolean won = false;
    private int tick;
    private int tickMs = 100;

    public MazeGame() {
        this(MazeLevel.builtIn(1), 1, false);
    }

    public MazeGame(MazeLevel level, int levelNumber, boolean custom) {
        this.level = level;
        this.levelNumber = levelNumber;
        this.custom = custom;
        load();
    }

    /** 从 Amoo 生成的 ASCII 文本创建自定义关卡。 */
    public static MazeGame fromAscii(String text) {
        return new MazeGame(MazeLevel.fromAscii(text), 0, true);
    }

    private void load() {
        System.arraycopy(level.walls, 0, walls, 0, CELLS);
        System.arraycopy(level.orbs, 0, orbs, 0, CELLS);
        ballX = level.startX;
        ballY = level.startY;
        exitX = level.exitX;
        exitY = level.exitY;
        totalOrbs = level.orbCount;
        collected = 0;
        lastDx = 0;
        lastDy = 0;
        moveCooldown = 0;
        alive = true;
        won = false;
        tick = 0;
    }

    @Override
    public String name() {
        return "迷宫滚球";
    }

    @Override
    public String hint() {
        return (custom ? "自定义关卡" : "第 " + levelNumber + " 关")
                + " · 倾斜滚动收集宝珠到出口 ★";
    }

    @Override
    public void reset() {
        load();
    }

    /** 通关后进入下一关（自定义关卡则原地重玩）。 */
    public void nextLevel() {
        if (custom) {
            load();
            return;
        }
        levelNumber++;
        level = MazeLevel.builtIn(levelNumber);
        load();
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public boolean isCustom() {
        return custom;
    }

    @Override
    public void onTilt(int dx, int dy) {
        // TiltController 每次只给单轴方向；(0,0) 表示回正停止
        lastDx = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        lastDy = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
    }

    @Override
    public void onAction() {
    }

    @Override
    public void tick() {
        if (won) return;
        tick++;
        if (moveCooldown > 0) {
            moveCooldown--;
            return;
        }
        if (lastDx == 0 && lastDy == 0) return;
        int nx = ballX + lastDx;
        int ny = ballY + lastDy;
        if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) return;
        int idx = ny * COLS + nx;
        if (walls[idx]) return;
        ballX = nx;
        ballY = ny;
        moveCooldown = 1;
        if (orbs[idx]) {
            orbs[idx] = false;
            collected++;
        }
        if (ballX == exitX && ballY == exitY && collected >= totalOrbs) {
            won = true;
            alive = false;
        }
    }

    @Override
    public void render(int[] frame) {
        for (int i = 0; i < CELLS; i++) frame[i] = 0;
        if (won) {
            for (int i = 0; i < CELLS; i++) frame[i] = 200;
            return;
        }
        boolean exitOpen = collected >= totalOrbs;
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                int idx = y * COLS + x;
                if (walls[idx]) frame[idx] = 42;
            }
        }
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                int idx = y * COLS + x;
                if (orbs[idx]) frame[idx] = (tick % 2 == 0) ? 220 : 150;
            }
        }
        frame[exitY * COLS + exitX] = exitOpen ? (tick % 2 == 0 ? 255 : 170) : 100;
        frame[ballY * COLS + ballX] = 255;
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
        String prefix = custom ? "自定义关卡" : "第 " + levelNumber + " 关";
        return prefix + "｜宝珠：" + collected + "/" + totalOrbs
                + (collected >= totalOrbs ? "　去出口 ★" : "");
    }
}

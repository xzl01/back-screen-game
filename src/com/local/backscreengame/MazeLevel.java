package com.local.backscreengame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 迷宫关卡数据：内置关卡（按关号生成）+ Amoo 生成的 ASCII 关卡。 */
public final class MazeLevel {
    public static final int COLS = 17;
    public static final int ROWS = 23;
    public static final int CELLS = COLS * ROWS;

    public final String name;
    public final boolean[] walls = new boolean[CELLS];   // true = 墙
    public final boolean[] orbs = new boolean[CELLS];    // true = 宝珠
    public final int startX, startY, exitX, exitY, orbCount;

    private MazeLevel(String name, boolean[] walls, boolean[] orbs,
                      int sx, int sy, int ex, int ey, int orbCount) {
        this.name = name;
        System.arraycopy(walls, 0, this.walls, 0, CELLS);
        System.arraycopy(orbs, 0, this.orbs, 0, CELLS);
        this.startX = sx;
        this.startY = sy;
        this.exitX = ex;
        this.exitY = ey;
        this.orbCount = orbCount;
    }

    /** 内置关卡：关号越大，宝珠越多（3 → 7 颗），迷宫种子固定可重玩。 */
    public static MazeLevel builtIn(int level) {
        Random rnd = new Random(2026000L + level * 7919L);
        boolean[] walls = generateMaze(rnd);

        int count = Math.min(3 + level, 7);
        boolean[] orbs = new boolean[CELLS];
        int placed = 0;
        int guard = 0;
        while (placed < count && guard++ < 5000) {
            int x = 1 + rnd.nextInt((COLS - 1) / 2) * 2;
            int y = 1 + rnd.nextInt((ROWS - 1) / 2) * 2;
            int idx = y * COLS + x;
            if (walls[idx] || orbs[idx]) continue;
            if (x == 1 && y == 1) continue;
            orbs[idx] = true;
            placed++;
        }
        return new MazeLevel("第 " + level + " 关", walls, orbs, 1, 1, COLS - 2, ROWS - 2, placed);
    }

    /** 递归回溯生成 17×23 迷宫。 */
    private static boolean[] generateMaze(Random rnd) {
        boolean[] walls = new boolean[CELLS];
        for (int i = 0; i < CELLS; i++) walls[i] = true;
        int cellsX = (COLS - 1) / 2;
        int cellsY = (ROWS - 1) / 2;
        boolean[] visited = new boolean[cellsX * cellsY];
        ArrayList<int[]> stack = new ArrayList<>();
        stack.add(new int[]{0, 0});
        visited[0] = true;
        walls[1 * COLS + 1] = false;
        while (!stack.isEmpty()) {
            int[] cur = stack.get(stack.size() - 1);
            int cx = cur[0];
            int cy = cur[1];
            int[] dirs = {0, 1, 2, 3};
            for (int i = 0; i < 4; i++) {
                int j = rnd.nextInt(4);
                int t = dirs[i];
                dirs[i] = dirs[j];
                dirs[j] = t;
            }
            boolean moved = false;
            for (int d : dirs) {
                int nx = cx + (d == 0 ? 1 : d == 1 ? -1 : 0);
                int ny = cy + (d == 2 ? 1 : d == 3 ? -1 : 0);
                if (nx < 0 || nx >= cellsX || ny < 0 || ny >= cellsY) continue;
                if (visited[ny * cellsX + nx]) continue;
                visited[ny * cellsX + nx] = true;
                int wx = cx * 2 + 1;
                int wy = cy * 2 + 1;
                int wnx = nx * 2 + 1;
                int wny = ny * 2 + 1;
                walls[wy * COLS + wx] = false;
                walls[wny * COLS + wnx] = false;
                walls[((wy + wny) / 2) * COLS + ((wx + wnx) / 2)] = false;
                stack.add(new int[]{nx, ny});
                moved = true;
                break;
            }
            if (!moved) stack.remove(stack.size() - 1);
        }
        return walls;
    }

    /**
     * 解析 Amoo 生成的 ASCII 迷宫。
     * 格式：17 列 × 23 行；'#'=墙，'.'=通路，'o'=宝珠，'P'=起点，'E'=出口。
     * 允许前后有解释文字、代码围栏，也兼容 17 行 × 23 列（自动转置）。
     */
    public static MazeLevel fromAscii(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("关卡内容为空");
        }
        String norm = text.replace("\r", "");
        String[] raw = norm.split("\n", -1);
        List<String> lines = new ArrayList<>();
        for (String s : raw) {
            String t = s.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("```")) continue;
            lines.add(t);
        }

        List<String> maze = pickMaze(lines, ROWS, COLS);
        if (maze == null) {
            List<String> transposedSource = pickMaze(lines, COLS, ROWS);
            if (transposedSource != null) maze = transpose(transposedSource, ROWS, COLS);
        }
        if (maze == null || maze.size() != ROWS) {
            throw new IllegalArgumentException("需要 17 列 × 23 行的迷宫文本");
        }

        boolean[] walls = new boolean[CELLS];
        boolean[] orbs = new boolean[CELLS];
        int sx = -1, sy = -1, ex = -1, ey = -1;
        int orbCount = 0;
        for (int y = 0; y < ROWS; y++) {
            String row = maze.get(y);
            for (int x = 0; x < COLS; x++) {
                char c = row.charAt(x);
                int idx = y * COLS + x;
                switch (c) {
                    case '#':
                    case 'X':
                    case '█':
                        walls[idx] = true;
                        break;
                    case 'P':
                    case 'S':
                    case 'p':
                    case 's':
                        sx = x;
                        sy = y;
                        break;
                    case 'E':
                    case 'G':
                    case 'e':
                    case 'g':
                        ex = x;
                        ey = y;
                        break;
                    case 'o':
                    case 'O':
                    case '*':
                    case '·':
                        orbs[idx] = true;
                        orbCount++;
                        break;
                    default:
                        break;   // '.', ' ', '-', '_' 等都视为通路
                }
            }
        }
        if (sx < 0 || sy < 0) { sx = 1; sy = 1; }
        if (ex < 0 || ey < 0) { ex = COLS - 2; ey = ROWS - 2; }
        walls[sy * COLS + sx] = false;
        walls[ey * COLS + ex] = false;
        if (orbCount == 0) {
            throw new IllegalArgumentException("迷宫中没有宝珠（o）");
        }
        return new MazeLevel("自定义关卡", walls, orbs, sx, sy, ex, ey, orbCount);
    }

    /** 在文本行里找出连续的 rows 行、每行恰好 cols 个字符的方块。 */
    private static List<String> pickMaze(List<String> lines, int rows, int cols) {
        for (int start = 0; start + rows <= lines.size(); start++) {
            boolean ok = true;
            for (int i = 0; i < rows; i++) {
                if (lines.get(start + i).length() != cols) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                List<String> out = new ArrayList<>();
                for (int i = 0; i < rows; i++) out.add(lines.get(start + i));
                return out;
            }
        }
        return null;
    }

    private static List<String> transpose(List<String> src, int rows, int cols) {
        List<String> out = new ArrayList<>();
        for (int y = 0; y < rows; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < cols; x++) sb.append(src.get(x).charAt(y));
            out.add(sb.toString());
        }
        return out;
    }
}

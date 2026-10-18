package com.local.backscreengame;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/** 通用游戏界面：按 intent extra 选择具体游戏，背屏显示画面，正面负责控制设置。 */
public final class GameActivity extends Activity implements TiltController.Listener {
    private static final String TAG = "BackScreenGame";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] frame = new int[SnakeGame.CELLS];

    private BackScreen backScreen;
    private TiltController tilt;
    private GameView game;
    private PowerManager.WakeLock wakeLock;

    private TextView statusView;
    private TextView titleView;
    private TextView hintView;
    private View mazeSection;
    private TextView amooStatus;
    private Button flipXButton;
    private Button flipYButton;

    private boolean running;
    private long createdUptime;
    private boolean invertX;
    private boolean invertY;
    private String apiError = "";

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running || game == null) return;
            game.tick();
            game.render(frame);
            backScreen.send(frame);
            updateStatus();
            if (game.isAlive()) {
                handler.postDelayed(this, game.tickMs());
            } else {
                running = false;
                backScreen.send(frame);
                updateStatus();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createdUptime = SystemClock.uptimeMillis();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String which = getIntent() == null ? "snake" : getIntent().getStringExtra("game");
        if (which == null) which = "snake";
        String emoji;
        String actionText;
        switch (which) {
            case "breakout":
                game = new BreakoutGame();
                emoji = "🧱";
                actionText = "发球 / 动作键";
                break;
            case "pong":
                game = new PongGame();
                emoji = "🏓";
                actionText = "动作键（发球/确认）";
                break;
            case "shooter":
                game = new ShooterGame();
                emoji = "🚀";
                actionText = "射击 / 音量键";
                break;
            case "maze": {
                String custom = getIntent() == null ? null : getIntent().getStringExtra("custom_level");
                MazeGame maze;
                if (custom != null) {
                    try {
                        maze = MazeGame.fromAscii(custom);
                    } catch (IllegalArgumentException e) {
                        maze = new MazeGame(MazeLevel.builtIn(1), 1, false);
                    }
                } else {
                    int saved = getSharedPreferences("backscreen", MODE_PRIVATE).getInt("maze_level", 1);
                    if (saved < 1) saved = 1;
                    maze = new MazeGame(MazeLevel.builtIn(saved), saved, false);
                }
                game = maze;
                emoji = "🌀";
                actionText = "本游戏倾斜即可";
                break;
            }
            case "flappy":
                game = new FlappyGame();
                emoji = "🐤";
                actionText = "拍翅膀 / 音量键";
                break;
            default:
                game = new SnakeGame();
                emoji = "🐍";
                actionText = "动作键（发球/确认）";
                break;
        }

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BackScreenGame:play");
        wakeLock.setReferenceCounted(false);

        backScreen = new BackScreen();
        if (!backScreen.init(this)) {
            apiError = backScreen.lastError;
            Log.e(TAG, "back screen init failed: " + apiError);
        }
        tilt = new TiltController(this, this);

        setContentView(R.layout.activity_game);
        titleView = findViewById(R.id.game_title);
        hintView = findViewById(R.id.game_hint);
        titleView.setText(emoji + " " + game.name());
        hintView.setText(game.hint());
        statusView = findViewById(R.id.game_status);
        ((Button) findViewById(R.id.btn_action)).setText(actionText);
        bindMazeSection();

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (game instanceof MazeGame && game.isWon()) {
                    MazeGame maze = (MazeGame) game;
                    maze.nextLevel();
                    if (!maze.isCustom()) {
                        getSharedPreferences("backscreen", MODE_PRIVATE).edit()
                                .putInt("maze_level", maze.getLevelNumber()).apply();
                    }
                }
                startGame();
            }
        });
        findViewById(R.id.btn_action).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (running && game != null) game.onAction();
            }
        });
        flipXButton = findViewById(R.id.btn_flip_x);
        flipXButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invertX = !invertX;
                flipXButton.setText("左右反转: " + (invertX ? "开" : "关"));
                tilt.setInvert(invertX, invertY);
            }
        });
        flipYButton = findViewById(R.id.btn_flip_y);
        flipYButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invertY = !invertY;
                flipYButton.setText("上下反转: " + (invertY ? "开" : "关"));
                tilt.setInvert(invertX, invertY);
            }
        });
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopGame();
                finish();
            }
        });

        updateStatus();
    }

    /** 迷宫页专属：Amoo 提示词 + 从剪贴板导入关卡。 */
    private void bindMazeSection() {
        mazeSection = findViewById(R.id.maze_amoo_section);
        amooStatus = findViewById(R.id.amoo_status);
        if (!(game instanceof MazeGame)) {
            mazeSection.setVisibility(View.GONE);
            return;
        }
        mazeSection.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_copy_prompt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyAmooPrompt();
            }
        });
        findViewById(R.id.btn_import_level).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importLevelFromClipboard();
            }
        });
    }

    private void copyAmooPrompt() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) {
            amooStatus.setText("剪贴板不可用");
            return;
        }
        cm.setPrimaryClip(ClipData.newPlainText("Amoo 迷宫提示词", getString(R.string.amoo_maze_prompt)));
        amooStatus.setText("提示词已复制，去 Amoo 粘贴生成，再把生成的迷宫复制回来导入");
    }

    private void importLevelFromClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm == null ? null : cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            amooStatus.setText("剪贴板为空：先用 Amoo 生成迷宫文本并复制");
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        try {
            MazeLevel level = MazeLevel.fromAscii(text == null ? "" : text.toString());
            stopGame();
            game = new MazeGame(level, 0, true);
            hintView.setText(game.hint());
            createdUptime = 0;   // 绕过进入页面时的防误触保护
            amooStatus.setText("已导入自定义关卡（宝珠 " + level.orbCount + " 颗）");
            startGame();
        } catch (IllegalArgumentException e) {
            amooStatus.setText("导入失败：" + e.getMessage());
        }
    }

    private void startGame() {
        if (SystemClock.uptimeMillis() - createdUptime < 400) return;
        if (apiError != null && !apiError.isEmpty()) {
            updateStatus();
            return;
        }
        game.reset();
        if (!backScreen.start()) {
            apiError = backScreen.lastError;
            updateStatus();
            return;
        }
        tilt.setInvert(invertX, invertY);
        tilt.start();
        running = true;
        if (!wakeLock.isHeld()) wakeLock.acquire(30 * 60 * 1000L);
        handler.removeCallbacks(tickRunnable);
        handler.post(tickRunnable);
        updateStatus();
    }

    private void stopGame() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        if (tilt != null) tilt.stop();
        if (backScreen != null) backScreen.stop();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        updateStatus();
    }

    @Override
    public void onDirection(int dx, int dy) {
        if (running && game != null && game.isAlive()) game.onTilt(dx, dy);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            stopGame();
            finish();
            return true;
        }
        if (running && game != null) game.onAction();
        return true;
    }

    private void startButtonLabel(Button b, String text) {
        b.setText(text);
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        if (apiError != null && !apiError.isEmpty()) {
            sb.append("背屏 API 不可用：\n").append(apiError).append('\n');
        } else if (game != null) {
            sb.append(game.status()).append('\n');
            if (!game.isAlive()) {
                sb.append(game.isWon() ? "通关！" : "游戏结束，点「开始/重开」");
            } else if (running) {
                sb.append("游戏中");
            } else {
                sb.append("点「开始/重开」开始");
            }
            sb.append('\n');
        }
        sb.append("加速度计：").append(tilt != null && tilt.hasSensor() ? "可用（" + tilt.status + "）" : "不可用");
        statusView.setText(sb.toString());
        View start = findViewById(R.id.btn_start);
        if (start instanceof Button) {
            if (game instanceof MazeGame && game.isWon()) {
                startButtonLabel((Button) start, ((MazeGame) game).isCustom() ? "再玩一次" : "下一关");
            } else {
                startButtonLabel((Button) start, "开始 / 重开");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopGame();
    }
}

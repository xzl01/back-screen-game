package com.local.backscreengame;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/** Back Screen Game 主菜单。 */
public final class MainActivity extends Activity {
    private BackScreen preview;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        statusView = findViewById(R.id.menu_status);
        preview = new BackScreen();
        if (!preview.init(this)) {
            statusView.setText("背屏 API 不可用：" + preview.lastError);
        } else {
            preview.restorePending();
            statusView.setText("背屏 API：正常 · 选个游戏开始");
        }

        bindGame(R.id.card_snake, "snake");
        bindGame(R.id.card_breakout, "breakout");
        bindGame(R.id.card_pong, "pong");
        bindGame(R.id.card_shooter, "shooter");
        bindGame(R.id.card_maze, "maze");
        bindGame(R.id.card_flappy, "flappy");
        findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testPattern();
            }
        });
        handleImportIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleImportIntent(intent);
    }

    /** 支持：分享文本到本 App（Amoo 分享）、backscreengame://import?level=... 深链接。 */
    private void handleImportIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) importLevel(text);
        } else if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null && "backscreengame".equals(data.getScheme())) {
                String level = data.getQueryParameter("level");
                if (level == null) level = data.getQueryParameter("l");
                if (level != null) importLevel(level);
            }
        }
    }

    private void importFromClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = cm == null ? null : cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            statusView.setText("剪贴板为空：先用 Amoo 生成迷宫文本并复制");
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        importLevel(text == null ? "" : text.toString());
    }

    private void importLevel(String text) {
        try {
            MazeLevel level = MazeLevel.fromAscii(text);
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("game", "maze");
            intent.putExtra("custom_level", text);
            startActivity(intent);
            statusView.setText("已导入自定义关卡（宝珠 " + level.orbCount + " 颗）");
        } catch (IllegalArgumentException e) {
            statusView.setText("导入失败：" + e.getMessage());
        }
    }

    private void bindGame(int id, final String key) {
        findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putExtra("game", key);
                startActivity(intent);
            }
        });
    }

    private void testPattern() {
        if (preview == null || !preview.ready) return;
        preview.start();
        int[] f = new int[SnakeGame.CELLS];
        for (int r = 0; r < SnakeGame.ROWS; r++) {
            for (int c = 0; c < SnakeGame.COLS; c++) {
                f[r * SnakeGame.COLS + c] = ((r / 2 + c / 2) % 2 == 0) ? 255 : 40;
            }
        }
        preview.send(f);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                preview.stop();
            }
        }, 2500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (preview != null) preview.stop();
    }
}

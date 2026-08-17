package com.qidian.fockhook;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LogViewerActivity extends Activity {

    private TextView textView;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        textView = new TextView(this);
        textView.setTextSize(12);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE);
        textView.setPadding(16, 16, 16, 16);
        scrollView.addView(textView);

        Button refresh = new Button(this);
        refresh.setText("刷新日志");
        refresh.setOnClickListener(v -> loadLogs());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.FILL_HORIZONTAL);
        layout.addView(refresh, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        setContentView(layout);

        loadLogs();
    }

    private void loadLogs() {
        new Thread(() -> {
            List<String> lines = readLogFile();
            handler.post(() -> {
                if (lines.isEmpty()) {
                    textView.setText("暂无日志。\n\n如果已经启用模块并操作过起点 App，请尝试点击刷新。");
                } else {
                    textView.setText(String.join("\n", lines));
                }
            });
        }).start();
    }

    private List<String> readLogFile() {
        List<String> result = new ArrayList<>();

        File direct = new File("/data/data/com.qidian.QDReader/files/fock_hook.log");
        try {
            if (direct.canRead()) {
                result.addAll(readFile(direct));
                if (!result.isEmpty()) return result;
            }
        } catch (Throwable ignored) {
        }

        try {
            Process process = new ProcessBuilder("su", "-c", "cat /data/data/com.qidian.QDReader/files/fock_hook.log")
                    .redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
            process.waitFor();
            if (!result.isEmpty()) return result;
        } catch (Throwable ignored) {
        }

        try {
            File self = new File(getFilesDir(), "fock_hook.log");
            if (self.canRead()) {
                result.addAll(readFile(self));
            }
        } catch (Throwable ignored) {
        }

        return result;
    }

    private List<String> readFile(File file) throws Exception {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();
        return lines;
    }
}

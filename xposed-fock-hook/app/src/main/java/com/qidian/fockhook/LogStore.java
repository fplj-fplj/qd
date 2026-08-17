package com.qidian.fockhook;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogStore {
    private static final String TAG = "FockHook";
    private static final String FILE_NAME = "fock_hook.log";
    private static Context appContext;

    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static File getLogFile() {
        if (appContext == null) {
            return new File("/data/data/com.qidian.QDReader/files/" + FILE_NAME);
        }
        return new File(appContext.getFilesDir(), FILE_NAME);
    }

    public static void log(String line) {
        Log.i(TAG, line);
        writeFile(line);
    }

    private static synchronized void writeFile(String line) {
        try {
            File file = getLogFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            PrintWriter pw = new PrintWriter(osw);
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            pw.println("[" + time + "] " + line);
            pw.flush();
            pw.close();
        } catch (Throwable t) {
            Log.e(TAG, "write log failed", t);
        }
    }
}

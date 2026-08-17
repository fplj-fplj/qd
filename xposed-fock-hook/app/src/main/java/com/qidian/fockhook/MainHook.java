package com.qidian.fockhook;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "FockHook";
    private LoadPackageParam lpparam;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        this.lpparam = lpparam;
        if (!lpparam.packageName.equals("com.qidian.QDReader")) {
            return;
        }

        Log.i(TAG, "hook start: " + lpparam.packageName);
        try {
            Class<?> activityThread = XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader);
            Object app = XposedHelpers.callStaticMethod(activityThread, "currentApplication");
            LogStore.init((android.content.Context) app);
        } catch (Throwable ignored) {
            LogStore.init(null);
        }
        LogStore.log("hook start: " + lpparam.packageName);

        hookFockSign();
        hookFockUtil();
        hookFockRT();
    }

    private void hookFockSign() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.yuewen.fock.Fock", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "sign", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    logParam("Fock.sign", param.args);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    logResult("Fock.sign", param.getResult());
                }
            });
            logHook("Fock.sign");
        } catch (Throwable t) {
            logError("Fock.sign", t);
        }
    }

    private void hookFockUtil() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.qidian.QDReader.component.util.FockUtil", lpparam.classLoader);
            Class<?> requestBody = XposedHelpers.findClass("okhttp3.RequestBody", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "getH", String.class, String.class, requestBody, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    logParam("FockUtil.getH", param.args);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    logResult("FockUtil.getH", param.getResult());
                }
            });
            logHook("FockUtil.getH");
        } catch (Throwable t) {
            logError("FockUtil.getH", t);
        }

        try {
            Class<?> clazz = XposedHelpers.findClass("com.qidian.QDReader.component.util.FockUtil", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "getEncrypt", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    logParam("FockUtil.getEncrypt", param.args);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    logResult("FockUtil.getEncrypt", param.getResult());
                }
            });
            logHook("FockUtil.getEncrypt");
        } catch (Throwable t) {
            logError("FockUtil.getEncrypt", t);
        }
    }

    private void hookFockRT() {
        try {
            Class<?> clazz = XposedHelpers.findClass("com.yuewen.fockrt.FockRT", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clazz, "sn", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    logParam("FockRT.sn", param.args);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    logResult("FockRT.sn", param.getResult());
                }
            });
            logHook("FockRT.sn");
        } catch (Throwable t) {
            logError("FockRT.sn", t);
        }
    }

    private void logHook(String name) {
        Log.i(TAG, "hooked: " + name);
        XposedBridge.log("FockHook hooked: " + name);
        LogStore.log("hooked: " + name);
    }

    private void logError(String name, Throwable t) {
        Log.e(TAG, "hook failed: " + name, t);
        XposedBridge.log("FockHook hook failed: " + name + " " + t);
        LogStore.log("hook failed: " + name + " " + t);
    }

    private void logParam(String name, Object[] args) {
        StringBuilder sb = new StringBuilder(name + "(");
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.valueOf(args[i]));
            }
        }
        sb.append(")");
        Log.i(TAG, sb.toString());
        XposedBridge.log("FockHook " + sb);
        LogStore.log(sb.toString());
    }

    private void logResult(String name, Object result) {
        Log.i(TAG, name + " => " + result);
        XposedBridge.log("FockHook " + name + " => " + result);
        LogStore.log(name + " => " + result);
    }
}

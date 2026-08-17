# FockHook

Xposed/LSPosed module for logging Qidian Fock sign/encryption inputs and outputs.

## Features

- Hooks:
  - `com.yuewen.fock.Fock.sign`
  - `FockUtil.getH`
  - `FockUtil.getEncrypt`
  - `FockRT.sn`
- Writes logs to:
  - Logcat tag `FockHook`
  - Xposed log
  - `/data/data/com.qidian.QDReader/files/fock_hook.log`
- In-app log viewer:
  - Open the `FockHook` app after enabling the module
  - Tap "刷新日志" to view collected logs

## Build

### Locally with Android Studio

1. Open this directory as an Android project.
2. Build APK.
3. Install on a device with LSPosed.

### GitHub Actions

The repository contains `.github/workflows/build.yml`.

After pushing, open the Actions tab and download the `fock-hook-apk` artifact.

## Usage

1. Install the module APK.
2. Enable it in LSPosed.
3. Select scope: `com.qidian.QDReader`.
4. Reboot / restart the app.
5. Use the Qidian app normally.
6. Open `FockHook` app to view logs, or use `adb logcat -s FockHook`.

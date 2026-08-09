# AWT

AWT is a calm, local-first alarm clock for Android. It combines exact alarms, an immersive clock, a countdown timer, and a stopwatch in one original Aurora interface.

## Included

- Full-screen alarm editor with a dedicated four-digit keypad
- Six-digit keypad timer input with quick presets
- Position-aware disabled keypad digits that prevent invalid times before entry
- Swipe navigation between alarms, clock, and measurement screens
- One-shot and weekly repeating alarms
- Optional gradual volume and vibration
- Dedicated clock screen with seconds and the next alarm
- Eight original themes and four clock-face styles
- System sans-serif Arabic-numeral outlines with true contour morphing, plus optional segmented monoline morphing
- A one-second, point-free color trail around the clock and timer
- Selectable Aurora, Tidal Light, and Still backgrounds with saved preferences
- Seven background presets plus independent full-color palettes for the background and text
- Optional clock halo, saved with the other appearance settings
- Always-visible timer and stopwatch controls overlaid on the large time display
- Timer pause and resume with the remaining time preserved on-device
- Crest-following low-brightness whitewater that continuously relaxes into residual foam
- Foreground-only motion with battery-saver and reduced-motion handling
- Full-screen ringing UI with a central 10-minute snooze action and a separate stop action
- Snooze from either volume key or by turning the phone face down
- Exact alarm scheduling through `AlarmManager.setAlarmClock()`
- Re-registration after reboot, time changes, app updates, and permission changes
- Animated hour/minute/second countdown with exact system-alarm delivery
- In-app stopwatch, lap list, and thumb-reachable bottom mode switch
- Home-screen widget showing the next enabled alarm
- Local SQLite storage with Android backup support
- Notification, exact-alarm, and full-screen-intent permission guidance
- Foreground-only display update loops to limit unnecessary battery use

The product and UI requirements are documented in
[docs/AWT_PRODUCT_SPEC.md](docs/AWT_PRODUCT_SPEC.md).

## Build

Requirements:

- Android Studio Quail 2 (2026.1.2) or newer
- JDK 17 or newer
- Android SDK Platform 36

Open this directory in Android Studio and let Gradle sync, or run:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## 実機へのインストール

実機確認用APKは、プロジェクト直下の
`AWT-v0.11.0-device-test.apk` です。Android 8.0（API 26）以降に対応しています。
これはAndroidのデバッグ鍵で署名したテスト専用ビルドです。

### USB経由

1. 端末で「開発者向けオプション」と「USBデバッグ」を有効にします。
2. PCへ接続し、端末に表示されるUSBデバッグ許可を承認します。
3. このディレクトリで次を実行します。

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r ".\AWT-v0.11.0-device-test.apk"
```

複数の端末やエミュレータが接続中の場合は、1行目で実機のシリアル番号を確認し、
次のように対象を指定します。

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s <実機のシリアル番号> install -r ".\AWT-v0.11.0-device-test.apk"
```

### 端末単体

APKをUSB転送、クラウドストレージ、メールなどで端末へコピーし、
端末のファイルアプリから開きます。Androidから確認された場合のみ、
そのファイルアプリに「不明なアプリのインストール」を一時的に許可してください。
インストール後はこの許可を戻せます。

### 初回の実機確認

1. アプリを起動し、画面上部の案内から通知、正確なアラーム、全画面通知を許可します。
2. 4桁テンキーで現在時刻の2分後にアラームを設定します。
3. 画面をロックして待ち、鳴動画面、スヌーズ、停止を確認します。
4. タイマーを1分で実行し、ストップウォッチの開始、ラップ、停止も確認します。
5. 重要な用途に使う前に、端末再起動後と省電力モードでも再度確認します。

## Device checks

Grant all three prompts shown in the app: notifications, exact alarms, and full-screen alarm notifications. On vendor-customized Android devices, also exclude AWT from aggressive battery optimization if alarms are delayed.

Before relying on AWT for important wake-ups, verify it on the intended device with the screen locked, in Doze, after reboot, and with silent/DND modes configured as expected.

## Privacy

AWT requests no internet permission. Alarm data and timer state stay on the device, except when the user has enabled Android's system backup or device-transfer feature.

## License

Source code in this repository is available under the Apache License 2.0. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

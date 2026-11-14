#!/usr/bin/env bash
# Run from the repo root AFTER `npx cap add android`. Adds the native home/lock-screen streak widget.
set -euo pipefail
APP=android/app/src/main
PKG=$APP/java/com/kidus/studyquest
mkdir -p "$PKG" "$APP/res/layout" "$APP/res/xml" "$APP/res/drawable" "$APP/res/values"
cp android-widget/StreakWidgetProvider.java android-widget/StreakWidgetPlugin.java android-widget/MainActivity.java "$PKG/"
cp android-widget/widget_streak.xml "$APP/res/layout/"
cp android-widget/streak_widget_info.xml "$APP/res/xml/"
cp android-widget/widget_bg.xml android-widget/sw_*.xml "$APP/res/drawable/"
cp android-widget/widget_strings.xml "$APP/res/values/"
cp android-widget/widget_colors.xml "$APP/res/values/"
python3 - <<'PY'
p = "android/app/src/main/AndroidManifest.xml"
s = open(p, encoding="utf-8").read()
if "StreakWidgetProvider" not in s:
    r = '''
        <receiver android:name=".StreakWidgetProvider" android:exported="false" android:label="Study Quest streak">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data android:name="android.appwidget.provider" android:resource="@xml/streak_widget_info" />
        </receiver>
    '''
    assert "</application>" in s
    s = s.replace("</application>", r + "</application>", 1)
    open(p, "w", encoding="utf-8").write(s)
print("widget added")
PY

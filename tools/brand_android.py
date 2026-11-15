# Englow: Android ikon, açılış ekranı, izinler, sürüm ve imza
import os, re, shutil, glob
from PIL import Image
RES = "android/app/src/main/res"
logo = Image.open("tools/logo_full.png").convert("RGBA")
for d, s in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    dst = os.path.join(RES, "mipmap-" + d); os.makedirs(dst, exist_ok=True)
    for f in glob.glob(dst + "/ic_launcher*.png"): os.remove(f)
    ic = logo.resize((s, s), Image.LANCZOS)
    ic.save(os.path.join(dst, "ic_launcher.png")); ic.save(os.path.join(dst, "ic_launcher_round.png"))
shutil.rmtree(os.path.join(RES, "mipmap-anydpi-v26"), ignore_errors=True)
for f in glob.glob(os.path.join(RES, "drawable*", "splash.png")):
    w, h = Image.open(f).size
    bg = Image.new("RGB", (w, h), (11, 31, 92)); s = int(min(w, h) * 0.8)
    lg = logo.resize((s, s), Image.LANCZOS)
    bg.paste(lg, ((w - s) // 2, (h - s) // 2), lg); bg.save(f)
g = "android/app/build.gradle"; t = open(g).read()
t = re.sub(r"versionCode \d+", "versionCode 6", t); t = re.sub(r'versionName "[^"]*"', 'versionName "2.1"', t)
sign = """
    signingConfigs {
        debug {
            storeFile file("../../tools/englow.keystore")
            storePassword "englowFCS2026"
            keyAlias "englow"
            keyPassword "englowFCS2026"
            enableV1Signing true
            enableV2Signing true
        }
    }
"""
if "englow.keystore" not in t: t = t.replace("android {", "android {" + sign, 1)
open(g, "w").write(t)
v = os.path.join(RES, "values", "styles.xml")
if os.path.exists(v):
    x = open(v).read()
    if "android:statusBarColor" not in x:
        x = x.replace('<style name="AppTheme.NoActionBar" parent="Theme.AppCompat.DayNight.NoActionBar">', '<style name="AppTheme.NoActionBar" parent="Theme.AppCompat.DayNight.NoActionBar">\n        <item name="android:statusBarColor">#0b1f5c</item>\n        <item name="android:navigationBarColor">#0b1f5c</item>')
        open(v, "w").write(x)
mf = "android/app/src/main/AndroidManifest.xml"; x = open(mf).read()
perms = ["android.permission.RECORD_AUDIO", "android.permission.MODIFY_AUDIO_SETTINGS"]
add = "".join(f'    <uses-permission android:name="{p}" />\n' for p in perms if p not in x)
q = '    <queries>\n        <intent><action android:name="android.speech.RecognitionService" /></intent>\n        <intent><action android:name="android.intent.action.TTS_SERVICE" /></intent>\n    </queries>\n'
x = x.replace("</manifest>", add + ("" if "<queries>" in x else q) + '    <uses-feature android:name="android.hardware.microphone" android:required="false" />\n</manifest>')
open(mf, "w").write(x)
print("branding ok")

# TalkTime: Android ikon, açılış ekranı ve sürüm ayarları
import os, re, shutil, glob
from PIL import Image
RES = "android/app/src/main/res"
for d in glob.glob("android-res/mipmap-*"):
    dst = os.path.join(RES, os.path.basename(d)); os.makedirs(dst, exist_ok=True)
    for f in glob.glob(d + "/*.png"): shutil.copy(f, dst)
shutil.rmtree(os.path.join(RES, "mipmap-anydpi-v26"), ignore_errors=True)
for f in glob.glob(os.path.join(RES, "mipmap-*", "ic_launcher_foreground.png")): os.remove(f)
logo = Image.open("tools/logo512.png").convert("RGB")
for f in glob.glob(os.path.join(RES, "drawable*", "splash.png")):
    w, h = Image.open(f).size
    bg = Image.new("RGB", (w, h), (6, 7, 26)); s = int(min(w, h) * 0.42)
    bg.paste(logo.resize((s, s), Image.LANCZOS), ((w - s) // 2, (h - s) // 2)); bg.save(f)
g = "android/app/build.gradle"; t = open(g).read()
t = re.sub(r"versionCode \d+", "versionCode 4", t); t = re.sub(r'versionName "[^"]*"', 'versionName "1.3"', t)
open(g, "w").write(t)
v = os.path.join(RES, "values", "styles.xml")
if os.path.exists(v):
    x = open(v).read()
    if "android:statusBarColor" not in x:
        x = x.replace('<style name="AppTheme.NoActionBar" parent="Theme.AppCompat.DayNight.NoActionBar">', '<style name="AppTheme.NoActionBar" parent="Theme.AppCompat.DayNight.NoActionBar">\n        <item name="android:statusBarColor">#06071a</item>\n        <item name="android:navigationBarColor">#06071a</item>')
        open(v, "w").write(x)
mf = "android/app/src/main/AndroidManifest.xml"; x = open(mf).read()
perms = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION", "android.permission.READ_CONTACTS", "android.permission.CAMERA"]
add = "".join(f'    <uses-permission android:name="{p}" />\n' for p in perms if p not in x)
x = x.replace("</manifest>", add + '    <uses-feature android:name="android.hardware.camera" android:required="false" />\n    <uses-feature android:name="android.hardware.location.gps" android:required="false" />\n</manifest>')
open(mf, "w").write(x)
print("branding ok")

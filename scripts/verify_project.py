#!/usr/bin/env python3
"""Offline structural checks; not a substitute for Gradle compilation or device tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
manifest = ROOT / "app/src/main/AndroidManifest.xml"
activity = ROOT / "app/src/main/java/com/nyxsky/anydownloader/MainActivity.java"
app_gradle = ROOT / "app/build.gradle"
icon = ROOT / "app/src/main/res/drawable/ic_launcher.xml"
strings = ROOT / "app/src/main/res/values/strings.xml"

ET.parse(manifest)
ET.parse(icon)
ET.parse(strings)
java = activity.read_text(encoding="utf-8")
gradle = app_gradle.read_text(encoding="utf-8")
manifest_text = manifest.read_text(encoding="utf-8")
checks = {
    "manifest parses": manifest.exists(),
    "launcher activity declared": "android.intent.action.MAIN" in manifest_text,
    "internet permission": "android.permission.INTERNET" in manifest_text,
    "launcher icon declared": 'android:icon="@drawable/ic_launcher"' in manifest_text,
    "vector launcher icon parses": icon.exists(),
    "app name resource exists": "AnyDownloader" in strings.read_text(encoding="utf-8"),
    "trusted HTTPS host check": "TRUSTED_HOST.equalsIgnoreCase(uri.getHost())" in java,
    "main-frame WebView error handling": "request.isForMainFrame()" in java,
    "HTTP error handling": "onReceivedHttpError" in java,
    "DownloadManager integration": "DownloadManager.Request" in java,
    "download filename sanitization": "Character.isISOControl(ch)" in java,
    "same-host cookie forwarding": "getCookie(uri.toString())" in java,
    "validated internet detection": "NET_CAPABILITY_VALIDATED" in java,
    "network callback cleanup": "unregisterNetworkCallback" in java,
    "WebView lifecycle cleanup": "webView.destroy()" in java,
    "Android app namespace": "com.nyxsky.anydownloader" in gradle,
    "minimum Android API configured": "minSdk 24" in gradle,
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL") + "  " + name)
if not all(checks.values()):
    raise SystemExit(1)
print(f"\n{len(checks)} structural checks passed. Gradle compilation and device tests are not performed by this script.")

#!/usr/bin/env python3
"""Offline structural checks; not a substitute for Gradle compilation or device tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
manifest = ROOT / "app/src/main/AndroidManifest.xml"
activity = ROOT / "app/src/main/java/com/nyxsky/anydownloader/MainActivity.java"
app_gradle = ROOT / "app/build.gradle"
ET.parse(manifest)
java = activity.read_text(encoding="utf-8")
gradle = app_gradle.read_text(encoding="utf-8")
manifest_text = manifest.read_text(encoding="utf-8")
checks = {
    "manifest parses": manifest.exists(),
    "launcher activity declared": "android.intent.action.MAIN" in manifest_text,
    "internet permission": "android.permission.INTERNET" in manifest_text,
    "trusted HTTPS host check": "TRUSTED_HOST.equalsIgnoreCase(uri.getHost())" in java,
    "Android WebResourceError import": "import android.webkit.WebResourceError;" in java,
    "DownloadManager integration": "DownloadManager.Request" in java,
    "same-host cookie forwarding": "getCookie(uri.toString())" in java,
    "Android app namespace": "com.nyxsky.anydownloader" in gradle,
    "minimum Android API configured": "minSdk 24" in gradle,
}
for name, ok in checks.items():
    print(("PASS" if ok else "FAIL") + "  " + name)
if not all(checks.values()):
    raise SystemExit(1)
print(f"\n{len(checks)} structural checks passed. Gradle compilation and device tests are not performed by this script.")

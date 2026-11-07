# AnyDownloader Android

Android companion client for the upstream [AnyDownloader](https://github.com/nyxsky404/AnyDownloader) service.

## Architecture
The upstream project is a Python Streamlit + FastAPI + yt-dlp service, not an Android app or Android SDK. This project embeds its hosted interface in an Android WebView and adds native navigation, connectivity feedback, sharing, and Android DownloadManager handoff for compatible HTTPS downloads. Extraction remains server-side and requires internet access. This is not an offline downloader and does not bundle yt-dlp or FFmpeg.

Default service: `https://anydownloader-g9r8.onrender.com/`.

## Features
- Custom AnyDownloader vector launcher icon and branded app bar.
- WebView support for Streamlit JavaScript and DOM storage.
- HTTPS trusted-host navigation policy and external links for HTTP(S), telephone, and email.
- Verified-internet status, live network callbacks, loading progress, retry action, and main-frame HTTP/load error feedback.
- DownloadManager handoff with same-host cookies, user-agent, referer, and sanitized filename derived from response metadata.
- Back navigation, WebView state restoration, and lifecycle cleanup.

## Build
Requirements: Android Studio, JDK 17, Android SDK Platform 35, Build Tools 35.0.0, and Gradle 8.9.

```bash
python3 scripts/verify_project.py
gradle :app:lintDebug
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions runs structural checks, Android lint, and debug APK assembly, then uploads the artifact as `AnyDownloader-debug-apk`.

## Limitations
Streamlit may create `blob:` download URLs that Android DownloadManager cannot handle. Compatible direct HTTPS downloads from the configured service host are supported. If the backend redirects downloads to another host, that host must be reviewed and explicitly added to the allowlist. Full native media extraction, a download queue, and offline use are not implemented. Real-device download behavior must be tested separately.

Download only media you own or are authorized to save, and follow source-platform terms and applicable law.

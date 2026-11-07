# AnyDownloader Android

Android companion client for the upstream [AnyDownloader](https://github.com/nyxsky404/AnyDownloader) service.

## Architecture
The upstream project is a Python Streamlit + FastAPI + yt-dlp service, not an Android app or Android SDK. This project embeds its hosted interface in an Android WebView and adds native navigation, connection feedback, sharing, and Android DownloadManager handoff for compatible HTTPS downloads. Extraction remains server-side and requires internet access. This is not an offline downloader and does not bundle yt-dlp or FFmpeg.

Default service: `https://anydownloader-g9r8.onrender.com/`.

## Features
- Native Android shell with refresh and share actions.
- WebView support for Streamlit JavaScript and DOM storage.
- HTTPS trusted-host navigation policy.
- Loading, connectivity, and main-frame error status.
- DownloadManager handoff with same-host cookies, user-agent, and referer.
- Back navigation and WebView state restoration.

## Build
Requirements: Android Studio, JDK 17, Android SDK Platform 35, Build Tools 35.0.0, and Gradle 8.9.

```bash
gradle :app:lintDebug
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions workflow runs lint and assembles a debug APK, then uploads it as `AnyDownloader-debug-apk`.

## Limitations
Streamlit may create `blob:` download URLs that Android DownloadManager cannot handle. Compatible direct HTTPS downloads from the configured service host are supported. Full native media extraction, download queue management, and offline use are not implemented. Real-device download behavior must be tested separately.

Download only media you own or are authorized to save, and follow source-platform terms and applicable law.

# [ R A D I O V O I D ] — BBS Music Terminal

Underground BBS-style music terminal. Retro CRT looks, real music backend.

**🌐 Web terminal:** https://mickeyperry.github.io/radiovoid-bbs/

## 📲 Native Android App

**[⇩ Download RadioVoid-BBS.apk](https://mickeyperry.github.io/radiovoid-bbs/app/RadioVoid-BBS.apk)**

A fully native Android app (no webview) sharing the same library and chat:

- Streams the full track library (MP3/WAV + MOD/S3M/XM/IT tracker modules)
- Tracker playback via **libopenmpt** compiled natively for ARM64
- Background playback with media notification + lock-screen controls
- FFT spectrum visualizer, 7 CRT color themes, scanline overlay
- Local playlists, shoutbox chat with notifications, SysOp console

Install: download the APK, open it, allow "install unknown apps" if asked.

## Tech

- **Web:** single-file PWA, Supabase backend (DB + storage + auth), libopenmpt-wasm for tracker playback
- **Android:** pure Java + JNI, built entirely on-device with aapt2/d8/apksigner (no Android Studio, no Gradle)

Created by **[Mickey Perry](https://mickeyperry.github.io/)** — Motion Designer & Animator.

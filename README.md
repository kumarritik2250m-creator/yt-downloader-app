# YTdown (ytdlnis jaisa app)

Kotlin + Jetpack Compose + `yt-dlp` (via `youtubedl-android` library) based YouTube downloader.

## ⚠️ Important — Legal Note
YouTube ke Terms of Service ke mutabik, sirf apna khud ka content, Creative Commons, ya
public domain videos download karna safe hai. Copyrighted content download karna ToS
violation ho sakta hai. Ye project sirf educational/personal use ke liye hai.

## Kya-Kya Features Hain
- **Home tab** — search bar + recent searches history
- **Detail screen** — title/author/container ke sath Audio/Video tabs
- **Format list** — har quality ka container, codec, file size, id dikhta hai
- **Downloads tab** — sab downloaded audio/video files ki list
- **More tab** — settings-style menu
- Files hamesha **device ke public storage** (Movies/YTdown, Music/YTdown) me save hoti hain — app ke andar nahi, isliye phone gallery/file-manager me bhi milengi
- Share menu se seedha link bhejna (YouTube app > Share > YTdown)
- Live download progress (% aur ETA)
- Metadata + thumbnail embed (ffmpeg ke through)

## Setup Kaise Karein

### 1. Prerequisites
- **Android Studio** (latest stable — Koala ya usse naya) install karein
- JDK 17 (Android Studio ke sath bundled aata hai)
- Internet connection (Gradle dependencies download karne ke liye)

### 2. Project Open Karein
1. Is poore `YTDownloader` folder ko ek zip me daala gaya hai — usse extract karein
2. Android Studio kholein → **Open** → extract ki hui `YTDownloader` folder select karein
3. Android Studio khud gradle sync kar lega (pehli baar thoda time lagega — dependencies
   download hongi, especially `youtubedl-android` library jo internally yt-dlp binary
   bundle karti hai)

### 3. Run Karein
- Ek emulator ya real device (USB debugging on) connect karein
- Green **Run ▶** button dabayein
- App install ho jayegi

### 4. Build APK (Signed/Unsigned)
- Android Studio me: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- Output: `app/build/outputs/apk/debug/app-debug.apk`
- Ye APK phone me install kar sakte hain (Settings → allow unknown sources)

## Project Structure
```
YTDownloader/
├── app/
│   ├── src/main/java/com/example/ytdownloader/
│   │   ├── YTDownloaderApp.kt        → yt-dlp/ffmpeg init
│   │   ├── DownloadManager.kt        → yt-dlp calls (info fetch + download)
│   │   ├── DownloadViewModel.kt      → UI state management
│   │   ├── DownloadModels.kt         → data classes
│   │   ├── MainActivity.kt           → Compose UI
│   │   └── ui/theme/Theme.kt
│   ├── src/main/res/                 → strings, themes
│   └── build.gradle.kts              → dependencies
├── build.gradle.kts
└── settings.gradle.kts
```

## Common Issues

**"yt-dlp binary not found" ya crash on first launch**
→ Pehli baar app open karte hi background me yt-dlp binary extract hoti hai
  (`YTDownloaderApp.kt` me). 5-10 second wait karein, phir try karein.

**Download fail ho raha hai / "Unable to extract"**
→ YouTube apna player logic frequently change karta hai, isliye kabhi-kabhi
  `youtubedl-android` library ko update karna padta hai. `app/build.gradle.kts` me
  version number latest se replace kar dein (GitHub: junkfood02/youtubedl-android
  releases check karein).

**Storage permission error (Android 10-12)**
→ App settings me manually storage permission allow karein.

## Aage Kya Add Kar Sakte Hain (Optional Enhancements)
- Playlist/batch download (multiple URLs ek sath)
- Download history (Room database)
- Pause/Resume support (WorkManager ke through)
- Dark/Light theme toggle
- Subtitle download option
- In-app video player (downloaded files preview ke liye)

Agar in me se koi feature add karwana ho, bata dena — us hisse ka code bhi bana doonga.

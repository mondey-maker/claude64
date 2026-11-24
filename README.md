# Claude64

Super Mario 64 co-op for Android, built on the open-source **sm64coopdx** engine.

* Touchscreen controls (floating stick, A / B / Z / R / Start, C-buttons, Menu)
* Multiplayer — host a server or join one by IP / hostname + port
* Mods — add `.zip`, `.lua` or `.mod` files from the launcher
* Launcher menu: **Play · Host server · Join server · Mods · Import ROM**
* No game assets are bundled. You import **your own** Super Mario 64 (US) ROM
  (`.z64`, `.v64` or `.n64`, MD5 `20b854b239203baf6c961b850a4a51a2`); it stays on your phone.

## Build the APK

### Easiest: GitHub Actions
Push this folder to a GitHub repository. The workflow in `.github/workflows/build.yml` builds SDL2, Lua and the
game with the Android NDK, then runs Gradle. Download `Claude64-debug-apk` from the run's Artifacts and install it.

### Locally (Linux)
Install JDK 17, the Android SDK (platform 34, build-tools 34) and NDK r26, plus `cmake`, `make`, `python3`, `git`, `curl`, then:

    export ANDROID_NDK_HOME=/path/to/ndk/26.3.11579264
    ./scripts/build_native.sh     # SDL2 + Lua + patched sm64coopdx -> jniLibs / assets
    gradle assembleDebug          # app/build/outputs/apk/debug/app-debug.apk

## Layout
| Path | What |
|---|---|
| `app/src/main/java/com/claude64/app` | Launcher menu, ROM import, mod manager, server list, game activity |
| `app/src/main/java/org/libsdl/app` | SDL 2.30.8 Java glue (must match the native SDL tag) |
| `patches/sm64coopdx-android.patch` | Android target for sm64coopdx: GLES2 build, paths, touch controls, ROM screen |
| `scripts/build_native.sh` | Cross-builds the native libraries |

## How the pieces talk
The launcher starts `GameActivity` (an SDLActivity) with command-line arguments for the game:
`--playername NAME`, `--server PORT` or `--client HOST PORT`. Files live in the app's external files folder
(`Android/data/com.claude64.app/files/`): `user/` (ROM, saves, config) and `mods/`.

## Status
Written and reviewed but **not yet compiled into an APK** — the environment it was authored in had no Android SDK/NDK.
Expect to fix small build issues on the first CI run (most likely in `scripts/build_native.sh` or the sm64coopdx Makefile
Android target). The touch controller and Android platform code were syntax-checked against the sm64coopdx sources.
Servers hosted from a phone usually need port-forwarding for internet play; the same Wi-Fi works directly.
CoopNet (public server browser) is disabled in this first version (`COOPNET=0`).

Super Mario 64 is © Nintendo. Claude64 is an unofficial fan project and ships no Nintendo assets.

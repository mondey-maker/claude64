#!/usr/bin/env bash
# Builds the native half of Claude64 (SDL2 + Lua + the patched sm64coopdx -> libmain.so)
# and drops it into app/src/main/jniLibs + app/src/main/assets/game.
#
# Needs: ANDROID_NDK_HOME (NDK r26+), cmake, make, python3, git, curl, a host gcc/clang.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="${WORK:-$ROOT/build}"
API=21
SDL_TAG=release-2.30.8            # must match the Java files in app/src/main/java/org/libsdl
COOP_REPO=https://github.com/coop-deluxe/sm64coopdx.git
COOP_BASE=8cd6e59                 # upstream commit the patch was made against
: "${ANDROID_NDK_HOME:?set ANDROID_NDK_HOME to your NDK}"

TC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
export ANDROID_CC="$TC/bin/aarch64-linux-android${API}-clang"
export ANDROID_CXX="$TC/bin/aarch64-linux-android${API}-clang++"
export ANDROID_AR="$TC/bin/llvm-ar"
DEPS="$WORK/deps"; mkdir -p "$WORK" "$DEPS"

echo "== SDL2 ($SDL_TAG)"
if [ ! -f "$DEPS/lib/libSDL2.so" ]; then
  [ -d "$WORK/SDL" ] || git clone --depth 1 --branch "$SDL_TAG" https://github.com/libsdl-org/SDL.git "$WORK/SDL"
  cmake -S "$WORK/SDL" -B "$WORK/SDL/b" -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-$API \
    -DSDL_SHARED=ON -DSDL_STATIC=OFF -DSDL_TEST=OFF -DCMAKE_INSTALL_PREFIX="$DEPS"
  cmake --build "$WORK/SDL/b" -j"$(nproc)"
  cmake --install "$WORK/SDL/b"
fi

echo "== Lua 5.3"
if [ ! -f "$DEPS/lib/liblua53.a" ]; then
  [ -d "$WORK/lua-5.3.6" ] || curl -fsSL https://www.lua.org/ftp/lua-5.3.6.tar.gz | tar -xz -C "$WORK"
  (cd "$WORK/lua-5.3.6/src" && rm -f lua.c luac.c *.o &&
   for f in *.c; do "$ANDROID_CC" -O2 -fPIC -DLUA_USE_POSIX -c "$f"; done &&
   "$ANDROID_AR" rcs "$DEPS/lib/liblua53.a" *.o)
  mkdir -p "$DEPS/include"; cp "$WORK/lua-5.3.6/src/"{lua.h,luaconf.h,lualib.h,lauxlib.h,lua.hpp} "$DEPS/include/"
fi
export ANDROID_DEPS_DIR="$DEPS"

echo "== sm64coopdx + Claude64 patch"
if [ ! -d "$WORK/coopdx" ]; then
  git clone "$COOP_REPO" "$WORK/coopdx"
  git -C "$WORK/coopdx" checkout "$COOP_BASE"
  git -C "$WORK/coopdx" apply --whitespace=nowarn "$ROOT/patches/sm64coopdx-android.patch"
fi
make -C "$WORK/coopdx" -j"$(nproc)" TARGET_ANDROID=1 COOPNET=0 DISCORD_SDK=0 UPDATER=0 NO_CURL=1

LIB="$(find "$WORK/coopdx/build" -name libmain.so | head -n1)"
[ -n "$LIB" ] || { echo "libmain.so was not produced"; exit 1; }

echo "== staging into the Android project"
JNI="$ROOT/app/src/main/jniLibs/arm64-v8a"; mkdir -p "$JNI"
cp "$LIB" "$JNI/libmain.so"
cp "$DEPS/lib/libSDL2.so" "$JNI/libSDL2.so"
if "$TC/bin/llvm-readelf" -d "$JNI/libmain.so" | grep -q 'libc++_shared'; then
  cp "$TC/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so" "$JNI/"
fi
GAME="$ROOT/app/src/main/assets/game"; rm -rf "$GAME"; mkdir -p "$GAME"
for d in lang mods palettes dynos; do [ -d "$WORK/coopdx/$d" ] && cp -r "$WORK/coopdx/$d" "$GAME/"; done
echo "Native build done. Now run: gradle assembleDebug"

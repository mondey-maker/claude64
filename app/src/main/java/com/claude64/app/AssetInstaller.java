package com.claude64.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Unpacks assets/game (lang, mods, palettes, dynos...) to filesDir/game, where the native code reads it. */
final class AssetInstaller {
    private static final String PREFS = "claude64";
    private static final String KEY = "assets_version";

    private AssetInstaller() {}

    static boolean needsInstall(Context c) {
        return currentVersion(c) != c.getSharedPreferences(PREFS, 0).getLong(KEY, -1)
                || !new File(c.getFilesDir(), "game").isDirectory();
    }

    static void install(Context c) throws IOException {
        File dest = new File(c.getFilesDir(), "game");
        deleteRecursive(dest);
        copyDir(c.getAssets(), "game", dest);
        SharedPreferences.Editor e = c.getSharedPreferences(PREFS, 0).edit();
        e.putLong(KEY, currentVersion(c)).apply();
    }

    private static long currentVersion(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).lastUpdateTime;
        } catch (Exception ex) {
            return 0;
        }
    }

    private static void copyDir(AssetManager am, String path, File dest) throws IOException {
        String[] kids = am.list(path);
        if (kids == null || kids.length == 0) {
            copyFile(am, path, dest);
            return;
        }
        if (!dest.isDirectory() && !dest.mkdirs()) throw new IOException("mkdir " + dest);
        for (String k : kids) copyDir(am, path + "/" + k, new File(dest, k));
    }

    private static void copyFile(AssetManager am, String path, File dest) throws IOException {
        File parent = dest.getParentFile();
        if (parent != null) parent.mkdirs();
        try (InputStream in = am.open(path); OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    static void deleteRecursive(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteRecursive(k);
        f.delete();
    }
}

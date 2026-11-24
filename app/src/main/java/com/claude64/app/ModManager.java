package com.claude64.app;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Adds / lists / removes mods in <external files>/mods, the folder the game scans. */
final class ModManager {
    private ModManager() {}

    static File modsDir(Context c) {
        File base = c.getExternalFilesDir(null);
        if (base == null) base = c.getFilesDir();
        File d = new File(base, "mods");
        d.mkdirs();
        return d;
    }

    static List<String> list(Context c) {
        String[] names = modsDir(c).list();
        List<String> out = new ArrayList<>();
        if (names != null) {
            Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
            out.addAll(Arrays.asList(names));
        }
        return out;
    }

    static void remove(Context c, String name) {
        if (name.contains("/") || name.contains("..")) return;
        AssetInstaller.deleteRecursive(new File(modsDir(c), name));
    }

    /** Adds a .zip (extracted), or a single .lua / .mod file. */
    static String add(Context c, Uri uri) {
        String name = displayName(c, uri);
        String lower = name.toLowerCase();
        try {
            if (lower.endsWith(".zip")) {
                String folder = name.substring(0, name.length() - 4).replaceAll("[^A-Za-z0-9._ -]", "_");
                unzip(c, uri, new File(modsDir(c), folder));
            } else if (lower.endsWith(".lua") || lower.endsWith(".mod")) {
                try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                    write(in, new File(modsDir(c), name.replaceAll("[^A-Za-z0-9._ -]", "_")));
                }
            } else {
                return "Unsupported file: " + name + " (use .zip, .lua or .mod)";
            }
            return null;
        } catch (Exception e) {
            return "Could not add " + name + ": " + e.getMessage();
        }
    }

    private static void unzip(Context c, Uri uri, File dest) throws IOException {
        // Many mod zips contain one top-level folder. If so, unpack it directly under mods/.
        List<String> tops = new ArrayList<>();
        boolean rootFiles = false;
        try (ZipInputStream z = new ZipInputStream(c.getContentResolver().openInputStream(uri))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                String n = e.getName();
                int s = n.indexOf('/');
                if (s < 0) { if (!e.isDirectory()) rootFiles = true; }
                else if (!tops.contains(n.substring(0, s))) tops.add(n.substring(0, s));
            }
        }
        boolean strip = !rootFiles && tops.size() == 1;
        File target = strip ? modsDir(c) : dest;
        String canon = modsDir(c).getCanonicalPath() + File.separator;
        try (ZipInputStream z = new ZipInputStream(c.getContentResolver().openInputStream(uri))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                File f = new File(target, e.getName());
                if (!f.getCanonicalPath().startsWith(canon)) continue; // zip-slip guard
                if (e.isDirectory()) { f.mkdirs(); continue; }
                write(z, f);
            }
        }
    }

    private static void write(InputStream in, File f) throws IOException {
        File p = f.getParentFile();
        if (p != null) p.mkdirs();
        try (OutputStream out = new FileOutputStream(f)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    static String displayName(Context c, Uri uri) {
        try (Cursor cur = c.getContentResolver().query(uri, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                int i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return cur.getString(i);
            }
        } catch (Exception ignored) { }
        String p = uri.getLastPathSegment();
        return p != null ? p : "file";
    }
}

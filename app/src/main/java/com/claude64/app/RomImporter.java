package com.claude64.app;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

/** Imports a Super Mario 64 (US) ROM chosen by the user. Accepts .z64, .v64 and .n64 byte orders. */
final class RomImporter {
    static final String US_MD5 = "20b854b239203baf6c961b850a4a51a2";

    private RomImporter() {}

    static File userDir(Context c) {
        File base = c.getExternalFilesDir(null);
        if (base == null) base = c.getFilesDir();
        File d = new File(base, "user");
        d.mkdirs();
        return d;
    }

    static boolean hasRom(Context c) {
        return new File(userDir(c), "baserom.us.z64").isFile();
    }

    /** @return null on success, otherwise a message for the user. */
    static String importRom(Context c, Uri uri) {
        File out = new File(userDir(c), "baserom.us.z64.tmp");
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            if (in == null) return "Could not open that file.";
            byte[] buf = new byte[1 << 16];
            int n = readFully(in, buf, 4);
            if (n < 4) return "That file is too small to be a ROM.";
            int mode;
            if ((buf[0] & 0xff) == 0x80 && buf[1] == 0x37) mode = 0;          // .z64 (big endian)
            else if ((buf[0] & 0xff) == 0x37 && (buf[1] & 0xff) == 0x80) mode = 1; // .v64 (byte swapped)
            else if (buf[0] == 0x40 && buf[1] == 0x12) mode = 2;              // .n64 (little endian)
            else return "That does not look like an N64 ROM.";

            MessageDigest md = MessageDigest.getInstance("MD5");
            try (OutputStream os = new FileOutputStream(out)) {
                int have = 4;
                while (true) {
                    int got = readFully(in, buf, have, buf.length - have);
                    int total = have + got;
                    int usable = total & ~3;
                    convert(buf, usable, mode);
                    md.update(buf, 0, usable);
                    os.write(buf, 0, usable);
                    if (got < buf.length - have) break;
                    // carry the unaligned tail (none, since buffer size is a multiple of 4)
                    have = 0;
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b & 0xff));
            if (!US_MD5.equals(sb.toString())) {
                out.delete();
                return "This is not the vanilla Super Mario 64 (US) ROM.";
            }
            File dest = new File(userDir(c), "baserom.us.z64");
            dest.delete();
            if (!out.renameTo(dest)) return "Could not save the ROM.";
            return null;
        } catch (Exception e) {
            out.delete();
            return "ROM import failed: " + e.getMessage();
        }
    }

    private static int readFully(InputStream in, byte[] b, int len) throws IOException {
        return readFully(in, b, 0, len);
    }

    private static int readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        int t = 0;
        while (t < len) {
            int n = in.read(b, off + t, len - t);
            if (n < 0) break;
            t += n;
        }
        return t;
    }

    private static void convert(byte[] b, int len, int mode) {
        for (int i = 0; i + 3 < len; i += 4) {
            byte b0 = b[i], b1 = b[i + 1], b2 = b[i + 2], b3 = b[i + 3];
            if (mode == 1) { b[i] = b1; b[i + 1] = b0; b[i + 2] = b3; b[i + 3] = b2; }
            else if (mode == 2) { b[i] = b3; b[i + 1] = b2; b[i + 2] = b1; b[i + 3] = b0; }
        }
    }
}

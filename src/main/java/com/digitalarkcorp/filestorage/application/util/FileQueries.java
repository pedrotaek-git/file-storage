package com.digitalarkcorp.filestorage.application.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class FileQueries {

    private static final Pattern ILLEGAL = Pattern.compile("[\\r\\n\\t]");

    private FileQueries() {}

    public static String normalizeFilename(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("filename required");
        }
        String trimmed = name.trim();
        String cleaned = ILLEGAL.matcher(trimmed).replaceAll("_");
        return cleaned;
    }

    public static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("sha256 error", e);
        }
    }

    public static String sha256(InputStream in) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                md.update(buf, 0, r);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("sha256 error", e);
        }
    }

    public static String escapeRegex(String s) {
        if (s == null) return null;
        return Pattern.quote(s);
    }

    public static String randomLinkId() {
        return UUID.randomUUID().toString();
    }

    public static byte[] readAll(InputStream in) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                bos.write(buf, 0, r);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("readAll error", e);
        }
    }
}

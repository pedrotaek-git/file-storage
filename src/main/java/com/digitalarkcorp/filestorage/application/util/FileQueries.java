package com.digitalarkcorp.filestorage.application.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

public final class FileQueries {
    private FileQueries() {}

    public static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        TreeSet<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String t : tags) {
            if (t != null) {
                String v = t.trim();
                if (!v.isBlank()) s.add(v);
            }
        }
        return s.isEmpty() ? null : List.copyOf(s);
    }

    public static String escapeRegex(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if ("\\.^$|?*+[]{}()".indexOf(c) >= 0) b.append('\\');
            b.append(c);
        }
        return b.toString();
    }

    public static String newId() {
        return HexFormat.of().formatHex(UUID.randomUUID().toString().getBytes()).substring(0, 24);
    }

    public static String sha256Hex(BufferedInputStream bin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            bin.mark(Integer.MAX_VALUE);
            try (DigestInputStream dis = new DigestInputStream(bin, md)) {
                byte[] buf = new byte[8192];
                while (dis.read(buf) != -1) { }
            }
            byte[] digest = md.digest();
            bin.reset();
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

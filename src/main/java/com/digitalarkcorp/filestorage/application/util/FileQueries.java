package com.digitalarkcorp.filestorage.application.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class FileQueries {
    private FileQueries() {}

    public static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String t : tags) {
            if (t == null) continue;
            String v = t.trim();
            if (!v.isEmpty()) set.add(v);
        }
        return new ArrayList<>(set);
    }
}

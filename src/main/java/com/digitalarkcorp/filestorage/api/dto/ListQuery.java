package com.digitalarkcorp.filestorage.api.dto;

public record ListQuery(
        String tag,
        String q,
        SortBy sortBy,
        SortDir sortDir,
        int page,
        int size
) {
    public enum SortBy { FILENAME, CREATED_AT, TAG, CONTENT_TYPE, SIZE }
    public enum SortDir { ASC, DESC }
}

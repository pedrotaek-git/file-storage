package com.digitalarkcorp.filestorage.api.dto;

public record ListQuery(
        String tag,
        String filenameContains,
        SortBy sortBy,
        SortDir sortDir,
        int page,
        int size
) {
    public enum SortBy { FILENAME, CREATED_AT, SIZE }
    public enum SortDir { ASC, DESC }
}

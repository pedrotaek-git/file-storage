package com.digitalarkcorp.filestorage.api.dto;

public record ListQuery(
        String tag,
        String q,
        SortBy sortBy,
        SortDir sortDir,
        Integer page,
        Integer size
) {
    public enum SortBy { FILENAME, CREATED_AT, UPDATED_AT, SIZE }
    public enum SortDir { ASC, DESC }
}

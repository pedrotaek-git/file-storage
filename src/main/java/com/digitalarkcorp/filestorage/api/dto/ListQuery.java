package com.digitalarkcorp.filestorage.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ListQuery(
        String tag,
        String q,
        @NotNull SortBy sortBy,
        @NotNull SortDir sortDir,
        @Min(0) int page,
        @Min(1) int size
) {
    public enum SortBy { FILENAME, CREATED_AT, UPDATED_AT, SIZE }
    public enum SortDir { ASC, DESC }
}

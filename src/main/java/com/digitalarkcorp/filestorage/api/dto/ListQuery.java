package com.digitalarkcorp.filestorage.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ListQuery(
        @Size(min = 1, max = 128) String tag,
        SortBy sortBy,
        SortDir sortDir,
        @Min(0) Integer page,
        @Min(1) Integer size
) {}

package com.digitalarkcorp.filestorage.api.dto;

import com.digitalarkcorp.filestorage.domain.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UploadMetadata(
        @NotBlank
        @Size(max = 255, message = "filename must be ≤ 255 chars")
        String filename,
        @NotNull
        Visibility visibility,
        @Size(max = 5, message = "at most 5 tags")
        List<@NotBlank String> tags
) {}


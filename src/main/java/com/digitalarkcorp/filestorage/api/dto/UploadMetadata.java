package com.digitalarkcorp.filestorage.api.dto;

import com.digitalarkcorp.filestorage.domain.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UploadMetadata(
        @NotBlank String filename,
        @NotNull Visibility visibility,
        List<String> tags
) { }

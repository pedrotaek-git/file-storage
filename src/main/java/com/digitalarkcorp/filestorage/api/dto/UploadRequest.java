package com.digitalarkcorp.filestorage.api.dto;

import com.digitalarkcorp.filestorage.domain.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UploadRequest(
        @NotBlank @Size(min = 1, max = 255) String filename,
        @NotNull Visibility visibility,
        List<String> tags
) { }

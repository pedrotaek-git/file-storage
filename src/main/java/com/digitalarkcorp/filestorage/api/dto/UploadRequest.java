package com.digitalarkcorp.filestorage.api.dto;

import com.digitalarkcorp.filestorage.domain.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UploadRequest(
        @NotBlank String filename,
        @NotNull Visibility visibility,
        @Size(max = 5) List<@NotBlank String> tags,
        String contentType
) {}

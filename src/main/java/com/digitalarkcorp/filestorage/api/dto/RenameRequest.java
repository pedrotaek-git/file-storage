package com.digitalarkcorp.filestorage.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameRequest(
        @NotBlank
        @Size(min = 1, max = 255)
        String newFilename
) { }

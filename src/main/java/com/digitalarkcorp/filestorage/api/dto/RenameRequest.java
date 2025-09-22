package com.digitalarkcorp.filestorage.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameRequest(
        @NotBlank String newFilename
) {}

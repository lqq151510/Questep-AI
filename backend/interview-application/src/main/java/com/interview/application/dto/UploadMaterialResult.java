package com.interview.application.dto;

import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.model.Material;

public record UploadMaterialResult(Material material, AsyncTaskRecord task) {
}

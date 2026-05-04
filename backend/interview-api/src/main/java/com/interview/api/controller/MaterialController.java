package com.interview.api.controller;

import com.interview.api.support.CurrentUser;
import com.interview.application.dto.UploadMaterialResult;
import com.interview.application.service.MaterialApplicationService;
import com.interview.common.api.ApiResponse;
import com.interview.domain.model.Material;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/materials")
public class MaterialController {
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "PDF", "TXT", "MD", "DOC", "DOCX", "PPT", "PPTX", "XLS", "XLSX", "CSV", "JSON", "BIN"
    );

    private final MaterialApplicationService materialApplicationService;
    @Value("${app.storage.material-dir:./data/materials}")
    private String baseDir;
    public MaterialController(MaterialApplicationService materialApplicationService) { this.materialApplicationService = materialApplicationService; }

    @PostMapping("/upload")
    public ApiResponse<UploadMaterialResult> upload(@RequestParam("file") @NotNull MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("file is empty");
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 10MB");
        }
        String original = sanitizeFilename(file.getOriginalFilename());
        String ext = extractExtension(original);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("File type not allowed: " + ext + ". Allowed types: " + ALLOWED_EXTENSIONS);
        }
        Path dir = Path.of(baseDir, LocalDate.now().toString());
        Files.createDirectories(dir);
        Path target = dir.resolve(UUID.randomUUID() + "-" + original).normalize();
        if (!target.startsWith(dir.normalize())) {
            throw new IllegalArgumentException("Invalid file path");
        }
        file.transferTo(target);
        return ApiResponse.ok(materialApplicationService.uploadAndCreateParseTask(CurrentUser.id(), original, ext, target.toAbsolutePath().toString()));
    }

    @GetMapping
    public ApiResponse<List<Material>> list() { return ApiResponse.ok(materialApplicationService.list(CurrentUser.id())); }

    private String sanitizeFilename(String filename) {
        String fallback = "unknown.bin";
        if (!StringUtils.hasText(filename)) {
            return fallback;
        }
        String baseName;
        try {
            Path fileNamePath = Path.of(filename).getFileName();
            baseName = fileNamePath == null ? filename : fileNamePath.toString();
        } catch (InvalidPathException ex) {
            baseName = filename;
        }
        String sanitized = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!StringUtils.hasText(sanitized) || ".".equals(sanitized) || "..".equals(sanitized)) {
            return fallback;
        }
        return sanitized;
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "BIN";
        }
        return filename.substring(dotIndex + 1).toUpperCase();
    }
}

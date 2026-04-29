package com.tps.controller;

import com.tps.dto.ApiResponse;
import com.tps.dto.file.UploadResponse;
import com.tps.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ApiResponse<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("文件不能为空");
        return ApiResponse.success(fileService.upload(file));
    }

    @GetMapping("/ping")
    public ApiResponse<?> ping() {
        Path dir = Path.of(uploadDir);
        return ApiResponse.success(Map.of(
                "uploadDir", dir.toAbsolutePath().toString(),
                "exists", Files.exists(dir),
                "writable", Files.exists(dir) && Files.isWritable(dir)
        ));
    }

    @GetMapping("/resolve")
    public ApiResponse<?> resolve(@RequestParam String path) {
        return ApiResponse.success(new UploadResponse(fileService.toAbsoluteUrl(path), path));
    }
}

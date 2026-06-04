package com.productshow.system.service;

import com.productshow.system.domain.Models.FileRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {
  private final Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();

  public FileRecord store(MultipartFile file, String module, String uploadedBy) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("上传文件不能为空");
    }
    String originalName = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
    String safeModule = sanitize(module == null || module.isBlank() ? "common" : module);
    String safeName = sanitize(originalName);
    String objectKey = safeModule + "/" + System.currentTimeMillis() + "-" + UUID.randomUUID() + "-" + safeName;
    Path target = uploadRoot.resolve(objectKey).normalize();
    if (!target.startsWith(uploadRoot)) {
      throw new IllegalArgumentException("非法上传路径");
    }
    try {
      Files.createDirectories(target.getParent());
      file.transferTo(target);
    } catch (IOException exception) {
      throw new IllegalStateException("文件保存失败", exception);
    }
    String uploadedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    return new FileRecord(
        originalName,
        objectKey.replace('\\', '/'),
        "local",
        file.getSize(),
        "/uploads/" + objectKey.replace('\\', '/'),
        uploadedBy == null || uploadedBy.isBlank() ? "管理员" : uploadedBy,
        uploadedAt,
        safeModule
    );
  }

  private static String sanitize(String value) {
    return value.replaceAll("[^a-zA-Z0-9._\\-\\p{IsHan}]", "_");
  }
}

package com.productshow.system.service;

import com.productshow.system.domain.Models.FileRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {
  private final Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();
  private final JdbcTemplate jdbc;

  public FileStorageService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

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
    String normalizedKey = objectKey.replace('\\', '/');
    String uploader = uploadedBy == null || uploadedBy.isBlank() ? "管理员" : uploadedBy;
    String uploadedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    FileRecord record = new FileRecord(
        originalName,
        normalizedKey,
        "local",
        file.getSize(),
        "/uploads/" + normalizedKey,
        uploader,
        uploadedAt,
        safeModule
    );
    saveRecord(record);
    return record;
  }

  private void saveRecord(FileRecord record) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("original_name", record.originalName());
    values.put("object_key", record.objectKey());
    values.put("bucket_name", record.bucketName());
    values.put("file_size", record.fileSize());
    values.put("url", record.url());
    values.put("uploaded_by", record.uploadedBy());
    values.put("uploaded_at", record.uploadedAt());
    values.put("module", record.module());
    new SimpleJdbcInsert(jdbc).withTableName("file_record")
        .usingGeneratedKeyColumns("id")
        .execute(values);
  }

  private static String sanitize(String value) {
    return value.replaceAll("[^a-zA-Z0-9._\\-\\p{IsHan}]", "_");
  }
}

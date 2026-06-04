package com.productshow.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Models {
  private Models() {
  }

  public record Category(Long id, String name, int sortOrder) {
  }

  public record FileRecord(
      String originalName,
      String objectKey,
      String bucketName,
      long fileSize,
      String url,
      String uploadedBy,
      String uploadedAt,
      String module
  ) {
  }

  public record Product(
      Long id,
      Long categoryId,
      String categoryName,
      String name,
      String model,
      String summary,
      Map<String, String> parameters,
      String imageUrl,
      BigDecimal price,
      List<String> carouselImages,
      String detailHtml,
      List<FileRecord> attachments,
      String uploadedBy,
      String updatedAt
  ) {
  }

  public record ProductRelation(
      Long mainProductId,
      Long relatedProductId,
      String relatedProductName,
      int recommendedQuantity,
      String recommendationNote,
      boolean defaultSelected
  ) {
  }

  public record Solution(
      Long id,
      String title,
      String industry,
      String summary,
      String architecture,
      String imageUrl,
      List<Long> recommendedProductIds,
      List<String> carouselImages,
      String detailHtml,
      List<FileRecord> attachments,
      String uploadedBy,
      String updatedAt
  ) {
  }

  public record ProjectCase(
      Long id,
      String name,
      String location,
      String industry,
      String summary,
      String imageUrl,
      List<String> carouselImages,
      String detailHtml,
      List<FileRecord> attachments,
      String uploadedBy,
      String updatedAt
  ) {
  }

  public record Inquiry(
      Long id,
      @NotBlank String contactName,
      @NotBlank String phone,
      @NotBlank String company,
      @NotBlank String projectName,
      @NotBlank String projectLocation,
      @NotBlank String requirement,
      String status,
      Instant createdAt
  ) {
  }

  public record QuoteRequest(
      Long inquiryId,
      @NotBlank String projectName,
      String customerName,
      BigDecimal discountRate,
      @NotEmpty List<QuoteMeasurement> measurements
  ) {
  }

  public record QuoteMeasurement(
      @NotBlank String name,
      String categoryName,
      @NotBlank String productName,
      @Positive int quantity,
      @Positive BigDecimal unitPrice
  ) {
  }

  public record Quote(
      Long id,
      Long inquiryId,
      String quoteNo,
      String projectName,
      int version,
      BigDecimal discountRate,
      BigDecimal totalAmount,
      String status,
      List<QuoteItem> items,
      Instant createdAt
  ) {
  }

  public record QuoteItem(String measurement, String productName, int quantity, BigDecimal unitPrice, BigDecimal amount) {
  }

  public record ConfigTemplate(Long id, String templateName, String description, List<QuoteMeasurement> measurements) {
  }

  public record PageResult<T>(List<T> records, long total, int page, int size) {
  }

  public record AiDocument(Long id, String title, String sourceType, String status, Instant createdAt) {
  }

  public record ChatRequest(@NotBlank String question) {
  }

  public record LoginRequest(@NotBlank String code) {
  }

  public record LoginResult(String token, String openId, String unionId, String role, String displayName, List<String> permissions) {
  }
}

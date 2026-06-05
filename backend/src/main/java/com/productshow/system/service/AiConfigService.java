package com.productshow.system.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiConfigService {
  private final JdbcTemplate jdbc;

  public AiConfigService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<AiModelConfigView> list() {
    return jdbc.query("SELECT * FROM ai_model_config WHERE deleted = 0 ORDER BY id", (rs, rowNum) -> view(mapConfig(rs)));
  }

  public AiModelConfigView create(Map<String, Object> payload) {
    AiModelConfig config = fromPayload(null, payload, null);
    Map<String, Object> values = values(config);
    Number id = new SimpleJdbcInsert(jdbc).withTableName("ai_model_config")
        .usingGeneratedKeyColumns("id")
        .executeAndReturnKey(values);
    if (config.enabled()) markOnlyEnabled(id.longValue());
    return view(find(id.longValue()));
  }

  public AiModelConfigView update(Long id, Map<String, Object> payload) {
    AiModelConfig existing = find(id);
    AiModelConfig next = fromPayload(id, payload, existing);
    jdbc.update("""
            UPDATE ai_model_config
            SET provider = ?, provider_code = ?, adapter_type = ?, base_url = ?, chat_model = ?, embedding_model = ?,
                api_key = ?, key_status = ?, enabled = ?, remark = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted = 0
            """,
        next.provider(), next.providerCode(), next.adapterType(), next.baseUrl(), next.chatModel(), next.embeddingModel(),
        next.apiKey(), next.keyStatus(), next.enabled() ? 1 : 0, next.remark(), id);
    if (next.enabled()) markOnlyEnabled(id);
    return view(find(id));
  }

  public AiModelConfigView enable(Long id) {
    find(id);
    markOnlyEnabled(id);
    return view(find(id));
  }

  public Map<String, Object> clearKey(Long id) {
    find(id);
    jdbc.update("UPDATE ai_model_config SET api_key = '', key_status = '未配置', updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    return Map.of("cleared", true, "id", id);
  }

  public Map<String, Object> test(Map<String, Object> payload) {
    Long id = longValue(payload.get("id"), 0L);
    AiModelConfig existing = id > 0 ? find(id) : null;
    AiModelConfig config = fromPayload(existing == null ? null : existing.id(), payload, existing);
    if (config.apiKey().isBlank()) return Map.of("ok", false, "message", "API Key 未配置，无法测试连接。");
    if (config.chatModel().isBlank()) return Map.of("ok", false, "message", "对话模型名称不能为空。");
    if (!config.baseUrl().isBlank() && !config.baseUrl().startsWith("http")) {
      return Map.of("ok", false, "message", "Base URL 需要以 http 或 https 开头。");
    }
    return Map.of(
        "ok", true,
        "message", "配置格式有效。第一版为本地格式校验，真实连通性将在模型适配层接入后校验。",
        "provider", config.provider(),
        "chatModel", config.chatModel()
    );
  }

  public AiModelConfig active() {
    List<AiModelConfig> records = jdbc.query(
        "SELECT * FROM ai_model_config WHERE deleted = 0 AND enabled = 1 ORDER BY id LIMIT 1",
        (rs, rowNum) -> mapConfig(rs)
    );
    return records.isEmpty() ? null : records.get(0);
  }

  private void markOnlyEnabled(Long id) {
    jdbc.update("UPDATE ai_model_config SET enabled = 0, updated_at = CURRENT_TIMESTAMP WHERE deleted = 0");
    jdbc.update("UPDATE ai_model_config SET enabled = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted = 0", id);
  }

  private AiModelConfig find(Long id) {
    List<AiModelConfig> records = jdbc.query("SELECT * FROM ai_model_config WHERE id = ? AND deleted = 0",
        (rs, rowNum) -> mapConfig(rs), id);
    if (records.isEmpty()) throw new IllegalArgumentException("AI 模型配置不存在");
    return records.get(0);
  }

  private static AiModelConfig fromPayload(Long id, Map<String, Object> payload, AiModelConfig existing) {
    String incomingKey = text(payload.get("apiKey"), "");
    String nextKey = incomingKey.isBlank() && existing != null ? existing.apiKey() : incomingKey;
    boolean enabled = bool(payload.get("enabled"), existing != null && existing.enabled());
    return new AiModelConfig(
        id,
        text(payload.get("provider"), existing == null ? "OpenAI Compatible" : existing.provider()),
        text(payload.get("providerCode"), existing == null ? "Custom" : existing.providerCode()),
        text(payload.get("adapterType"), existing == null ? "OpenAI Compatible" : existing.adapterType()),
        text(payload.get("baseUrl"), existing == null ? "" : existing.baseUrl()),
        text(payload.get("chatModel"), existing == null ? "" : existing.chatModel()),
        text(payload.get("embeddingModel"), existing == null ? "" : existing.embeddingModel()),
        nextKey,
        nextKey.isBlank() ? "未配置" : "已配置",
        enabled,
        text(payload.get("remark"), existing == null ? "" : existing.remark()),
        now()
    );
  }

  private static Map<String, Object> values(AiModelConfig config) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("provider", config.provider());
    values.put("provider_code", config.providerCode());
    values.put("adapter_type", config.adapterType());
    values.put("base_url", config.baseUrl());
    values.put("chat_model", config.chatModel());
    values.put("embedding_model", config.embeddingModel());
    values.put("api_key", config.apiKey());
    values.put("key_status", config.keyStatus());
    values.put("enabled", config.enabled() ? 1 : 0);
    values.put("remark", config.remark());
    return values;
  }

  private static AiModelConfig mapConfig(ResultSet rs) throws SQLException {
    return new AiModelConfig(
        rs.getLong("id"),
        rs.getString("provider"),
        rs.getString("provider_code"),
        rs.getString("adapter_type"),
        rs.getString("base_url"),
        rs.getString("chat_model"),
        rs.getString("embedding_model"),
        rs.getString("api_key") == null ? "" : rs.getString("api_key"),
        rs.getString("key_status"),
        rs.getInt("enabled") == 1,
        rs.getString("remark"),
        rs.getTimestamp("updated_at") == null ? "" : rs.getTimestamp("updated_at").toInstant().toString()
    );
  }

  private static AiModelConfigView view(AiModelConfig config) {
    return new AiModelConfigView(
        config.id(),
        config.provider(),
        config.providerCode(),
        config.adapterType(),
        config.baseUrl(),
        config.chatModel(),
        config.embeddingModel(),
        mask(config.apiKey()),
        config.keyStatus(),
        config.enabled(),
        config.remark(),
        config.updatedAt()
    );
  }

  private static String mask(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) return "";
    if (apiKey.length() <= 8) return "****";
    return apiKey.substring(0, Math.min(4, apiKey.length())) + "****" + apiKey.substring(apiKey.length() - 4);
  }

  private static String text(Object value, String fallback) {
    return value == null || value.toString().isBlank() ? fallback : value.toString();
  }

  private static boolean bool(Object value, boolean fallback) {
    if (value instanceof Boolean bool) return bool;
    if (value == null) return fallback;
    return Boolean.parseBoolean(String.valueOf(value));
  }

  private static Long longValue(Object value, Long fallback) {
    if (value instanceof Number number) return number.longValue();
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (Exception ignored) {
      return fallback;
    }
  }

  private static String now() {
    return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }

  public record AiModelConfig(
      Long id,
      String provider,
      String providerCode,
      String adapterType,
      String baseUrl,
      String chatModel,
      String embeddingModel,
      String apiKey,
      String keyStatus,
      boolean enabled,
      String remark,
      String updatedAt
  ) {
  }

  public record AiModelConfigView(
      Long id,
      String provider,
      String providerCode,
      String adapterType,
      String baseUrl,
      String chatModel,
      String embeddingModel,
      String maskedApiKey,
      String keyStatus,
      boolean enabled,
      String remark,
      String updatedAt
  ) {
  }
}

package com.productshow.system.service;

import com.productshow.system.domain.Models.AiDocument;
import com.productshow.system.service.AiConfigService.AiModelConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiPlaceholderService {
  private final AiConfigService aiConfigService;
  private final JdbcTemplate jdbc;

  public AiPlaceholderService(AiConfigService aiConfigService, JdbcTemplate jdbc) {
    this.aiConfigService = aiConfigService;
    this.jdbc = jdbc;
  }

  public List<AiDocument> documents() {
    return jdbc.query("""
            SELECT id, title, source_type, status, created_at
            FROM ai_document_record
            WHERE deleted = 0
            ORDER BY id
            """, this::mapDocument);
  }

  public Object chat(String question) {
    AiModelConfig active = aiConfigService.active();
    Map<String, Object> record;
    if (active == null) {
      record = Map.of(
          "question", question,
          "answer", "当前尚未启用大模型配置。请到后台“系统设置 - AI 模型配置”中选择一个模型并设为启用。",
          "status", "MODEL_NOT_ENABLED",
          "createdAt", Instant.now().toString()
      );
    } else if (active.apiKey() == null || active.apiKey().isBlank()) {
      record = Map.of(
          "question", question,
          "answer", "当前启用的模型“" + active.provider() + "”尚未配置 API Key。请到后台系统设置中填写 Key 后再使用小恒问答。",
          "status", "API_KEY_MISSING",
          "provider", active.provider(),
          "chatModel", active.chatModel(),
          "createdAt", Instant.now().toString()
      );
    } else {
      record = Map.of(
          "question", question,
          "answer", "已读取当前启用模型“" + active.provider() + " / " + active.chatModel()
              + "”。第一版已完成模型配置持久化，真实知识库检索和模型调用将在下一步接入。",
          "status", "CONFIGURED_MOCK",
          "provider", active.provider(),
          "chatModel", active.chatModel(),
          "references", List.of("智能位移传感器技术说明", "边坡自动化监测方案白皮书"),
          "createdAt", Instant.now().toString()
      );
    }
    saveQuestionRecord(record);
    return record;
  }

  public List<Map<String, Object>> questionRecords() {
    return jdbc.query("""
            SELECT question, answer, status, provider, chat_model, created_at
            FROM ai_question_record
            ORDER BY id DESC
            LIMIT 50
            """, (rs, rowNum) -> {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("question", rs.getString("question"));
      row.put("answer", rs.getString("answer"));
      row.put("status", rs.getString("status"));
      row.put("provider", rs.getString("provider"));
      row.put("chatModel", rs.getString("chat_model"));
      row.put("createdAt", rs.getTimestamp("created_at").toInstant().toString());
      return row;
    });
  }

  private void saveQuestionRecord(Map<String, Object> record) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("question", record.get("question"));
    values.put("answer", record.get("answer"));
    values.put("status", record.get("status"));
    values.put("provider", record.getOrDefault("provider", ""));
    values.put("chat_model", record.getOrDefault("chatModel", ""));
    new SimpleJdbcInsert(jdbc).withTableName("ai_question_record")
        .usingGeneratedKeyColumns("id")
        .execute(values);
  }

  private AiDocument mapDocument(ResultSet rs, int rowNum) throws SQLException {
    return new AiDocument(
        rs.getLong("id"),
        rs.getString("title"),
        rs.getString("source_type"),
        rs.getString("status"),
        rs.getTimestamp("created_at").toInstant()
    );
  }
}

package com.productshow.system.service;

import com.productshow.system.domain.Models.AiDocument;
import com.productshow.system.service.AiConfigService.AiModelConfig;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AiPlaceholderService {
  private final AiConfigService aiConfigService;
  private final List<Map<String, Object>> questionRecords = new CopyOnWriteArrayList<>();

  public AiPlaceholderService(AiConfigService aiConfigService) {
    this.aiConfigService = aiConfigService;
  }

  public List<AiDocument> documents() {
    return List.of(
        new AiDocument(1L, "智能位移传感器技术说明", "RECORD", "RECORDED", Instant.now()),
        new AiDocument(2L, "边坡自动化监测方案白皮书", "RECORD", "RECORDED", Instant.now()),
        new AiDocument(3L, "尾矿库安全监测常见问答", "RECORD", "RECORDED", Instant.now())
    );
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
              + "”。第一版已完成模型配置切换，真实知识库检索和模型调用将在下一步接入。",
          "status", "CONFIGURED_MOCK",
          "provider", active.provider(),
          "chatModel", active.chatModel(),
          "references", List.of("智能位移传感器技术说明", "边坡自动化监测方案白皮书"),
          "createdAt", Instant.now().toString()
      );
    }
    questionRecords.add(record);
    return record;
  }

  public List<Map<String, Object>> questionRecords() {
    return questionRecords;
  }
}

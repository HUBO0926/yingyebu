package com.productshow.system.service;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AiConfigService {
  private final AtomicLong ids = new AtomicLong(20);
  private final CopyOnWriteArrayList<AiModelConfig> configs = new CopyOnWriteArrayList<>();

  public AiConfigService() {
    configs.addAll(List.of(
        new AiModelConfig(1L, "DeepSeek", "DeepSeek", "OpenAI Compatible",
            "https://api.deepseek.com", "deepseek-v4-flash", "BAAI/bge-m3", "", "未配置", false,
            "适合国内网络环境下做通用问答，后续可切换 V4 系列模型。", now()),
        new AiModelConfig(2L, "Gemini", "Gemini", "Native",
            "https://generativelanguage.googleapis.com", "gemini-3.1-flash-lite", "gemini-embedding-001", "", "未配置", false,
            "适合知识库问答和向量化，服务器需能访问 Google API。", now()),
        new AiModelConfig(3L, "阿里云百炼/Qwen", "DashScope", "OpenAI Compatible",
            "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus", "text-embedding-v4", "", "未配置", false,
            "国内优先推荐，兼容 OpenAI 调用方式，适合通义千问系列。", now()),
        new AiModelConfig(4L, "百度千帆/ERNIE", "Qianfan", "OpenAI Compatible",
            "https://qianfan.baidubce.com/v2", "ernie-4.0-turbo-8k", "bge-large-zh", "", "未配置", false,
            "按千帆控制台实际开通模型填写模型 ID。", now()),
        new AiModelConfig(5L, "火山方舟/Doubao", "VolcArk", "OpenAI Compatible",
            "https://ark.cn-beijing.volces.com/api/v3", "doubao-seed-1-6", "doubao-embedding", "", "未配置", false,
            "适合接入豆包和方舟上的第三方模型。", now()),
        new AiModelConfig(6L, "腾讯混元", "Hunyuan", "OpenAI Compatible",
            "https://api.hunyuan.cloud.tencent.com/v1", "hunyuan-turbo", "hunyuan-embedding", "", "未配置", false,
            "如账号接口不是兼容模式，后续使用 nativeAdapter 单独适配。", now()),
        new AiModelConfig(7L, "智谱 GLM", "Zhipu", "OpenAI Compatible",
            "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash", "embedding-3", "", "未配置", false,
            "适合 GLM 系列模型，支持按控制台模型 ID 调整。", now()),
        new AiModelConfig(8L, "月之暗面/Kimi", "Moonshot", "OpenAI Compatible",
            "https://api.moonshot.cn/v1", "moonshot-v1-8k", "", "", "未配置", false,
            "适合长文本问答，embedding 可先使用其他供应商。", now()),
        new AiModelConfig(9L, "MiniMax", "MiniMax", "OpenAI Compatible",
            "https://api.minimax.chat/v1", "MiniMax-Text-01", "", "", "未配置", false,
            "按 MiniMax 控制台实际模型名称调整。", now()),
        new AiModelConfig(10L, "讯飞星火", "Spark", "Native",
            "https://spark-api-open.xf-yun.com", "generalv3.5", "", "", "未配置", false,
            "预留原生适配入口，后续按星火鉴权方式单独接入。", now()),
        new AiModelConfig(11L, "硅基流动", "SiliconFlow", "OpenAI Compatible",
            "https://api.siliconflow.cn/v1", "Qwen/Qwen2.5-72B-Instruct", "BAAI/bge-m3", "", "未配置", false,
            "适合统一接入多个国产开源和闭源模型。", now()),
        new AiModelConfig(12L, "OpenAI Compatible", "Custom", "OpenAI Compatible",
            "", "", "", "", "未配置", false,
            "自定义兼容接口，可填写代理或私有模型网关。", now())
    ));
  }

  public List<AiModelConfigView> list() {
    return configs.stream().map(this::view).toList();
  }

  public AiModelConfigView create(Map<String, Object> payload) {
    AiModelConfig config = fromPayload(null, payload, null);
    configs.add(config);
    if (config.enabled()) markOnlyEnabled(config.id());
    return view(config);
  }

  public AiModelConfigView update(Long id, Map<String, Object> payload) {
    AiModelConfig existing = find(id);
    AiModelConfig next = fromPayload(id, payload, existing);
    replace(next);
    if (next.enabled()) markOnlyEnabled(next.id());
    return view(next);
  }

  public AiModelConfigView enable(Long id) {
    find(id);
    markOnlyEnabled(id);
    return view(find(id));
  }

  private void markOnlyEnabled(Long id) {
    for (int index = 0; index < configs.size(); index += 1) {
      AiModelConfig item = configs.get(index);
      configs.set(index, item.withEnabled(item.id().equals(id)));
    }
  }

  public Map<String, Object> clearKey(Long id) {
    AiModelConfig existing = find(id);
    replace(existing.withApiKey("").withUpdatedAt(now()));
    return Map.of("cleared", true, "id", id);
  }

  public Map<String, Object> test(Map<String, Object> payload) {
    Long id = longValue(payload.get("id"), 0L);
    AiModelConfig existing = id > 0 ? find(id) : null;
    AiModelConfig config = fromPayload(existing == null ? null : existing.id(), payload, existing);
    if (config.apiKey().isBlank()) {
      return Map.of("ok", false, "message", "API Key 未配置，无法测试连接。");
    }
    if (config.chatModel().isBlank()) {
      return Map.of("ok", false, "message", "对话模型名称不能为空。");
    }
    if (!config.baseUrl().isBlank() && !config.baseUrl().startsWith("http")) {
      return Map.of("ok", false, "message", "Base URL 需要以 http 或 https 开头。");
    }
    return Map.of(
        "ok", true,
        "message", "配置格式有效。第一版为本地模拟测试，真实连通性将在模型适配层接入后校验。",
        "provider", config.provider(),
        "chatModel", config.chatModel()
    );
  }

  public AiModelConfig active() {
    return configs.stream().filter(AiModelConfig::enabled).findFirst().orElse(null);
  }

  private AiModelConfig fromPayload(Long id, Map<String, Object> payload, AiModelConfig existing) {
    String incomingKey = text(payload.get("apiKey"), "");
    String nextKey = incomingKey.isBlank() && existing != null ? existing.apiKey() : incomingKey;
    boolean enabled = bool(payload.get("enabled"), existing != null && existing.enabled());
    return new AiModelConfig(
        id == null ? ids.incrementAndGet() : id,
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

  private AiModelConfig find(Long id) {
    return configs.stream().filter(item -> item.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("AI 模型配置不存在"));
  }

  private void replace(AiModelConfig next) {
    for (int index = 0; index < configs.size(); index += 1) {
      if (configs.get(index).id().equals(next.id())) {
        configs.set(index, next);
        return;
      }
    }
    configs.add(next);
  }

  private AiModelConfigView view(AiModelConfig config) {
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
    AiModelConfig withEnabled(boolean nextEnabled) {
      return new AiModelConfig(id, provider, providerCode, adapterType, baseUrl, chatModel, embeddingModel, apiKey,
          keyStatus, nextEnabled, remark, now());
    }

    AiModelConfig withApiKey(String nextApiKey) {
      return new AiModelConfig(id, provider, providerCode, adapterType, baseUrl, chatModel, embeddingModel, nextApiKey,
          nextApiKey.isBlank() ? "未配置" : "已配置", enabled, remark, now());
    }

    AiModelConfig withUpdatedAt(String nextUpdatedAt) {
      return new AiModelConfig(id, provider, providerCode, adapterType, baseUrl, chatModel, embeddingModel, apiKey,
          keyStatus, enabled, remark, nextUpdatedAt);
    }
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

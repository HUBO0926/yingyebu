package com.productshow.system.service;

import com.productshow.system.domain.Models.ConfigTemplate;
import com.productshow.system.domain.Models.Inquiry;
import com.productshow.system.domain.Models.PageResult;
import com.productshow.system.domain.Models.Quote;
import com.productshow.system.domain.Models.QuoteItem;
import com.productshow.system.domain.Models.QuoteMeasurement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SalesService {
  private static final DateTimeFormatter DISPLAY_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Value("${wechat.appid:}")
  private String wechatAppId;

  @Value("${wechat.secret:}")
  private String wechatSecret;

  private final AtomicLong inquiryIds = new AtomicLong(1000);
  private final AtomicLong quoteIds = new AtomicLong(2003);
  private final AtomicLong templateIds = new AtomicLong(2);
  private final AtomicLong personnelIds = new AtomicLong(4);

  private final List<Inquiry> inquiries = new CopyOnWriteArrayList<>();
  private final List<ManagedInquiry> managedInquiries = new CopyOnWriteArrayList<>();
  private final List<Quote> quotes = new CopyOnWriteArrayList<>();
  private final List<QuoteSummary> quoteSummaries = new CopyOnWriteArrayList<>();
  private final List<ConfigTemplate> templates = new CopyOnWriteArrayList<>();
  private final List<Personnel> personnel = new CopyOnWriteArrayList<>();

  public SalesService() {
    managedInquiries.addAll(List.of(
        new ManagedInquiry(1001L, "张工", "华北矿业集团", "北山边坡自动化监测", "待分配", "", "未分配", "2026-06-03 10:12"),
        new ManagedInquiry(1002L, "陈工", "西南水务", "水库大坝安全监测", "已分配", "sales-2", "李销售", "2026-06-02 16:05"),
        new ManagedInquiry(1003L, "刘工", "西北地勘院", "地质灾害位移观测站", "已分配", "sales-1", "王销售", "2026-06-01 09:30")
    ));

    seedQuote(2001L, "Q20260603001", "北山边坡自动化监测", "华北矿业集团", "sales-1", "王销售",
        new BigDecimal("69008.00"), "DRAFT", "2026-06-03 10:20");
    seedQuote(2002L, "Q20260602003", "西南水库大坝监测", "西南水务", "sales-2", "李销售",
        new BigDecimal("128600.00"), "GENERATED", "2026-06-02 15:50");
    seedQuote(2003L, "Q20260601002", "地灾位移观测站", "西北地勘院", "sales-1", "王销售",
        new BigDecimal("43200.00"), "SENT", "2026-06-01 09:08");

    templates.addAll(List.of(
        new ConfigTemplate(1L, "标准设备报价书", "适合小程序报价工具生成正式设备报价单。",
            List.of(new QuoteMeasurement("标准监测站", "传感器", "智能位移传感器", 2, new BigDecimal("6800.00")))),
        new ConfigTemplate(2L, "边坡基础监测模板", "适合中小型边坡项目的基础配置。",
            List.of(new QuoteMeasurement("坡体位移", "采集仪", "多通道数据采集仪", 1, new BigDecimal("12800.00"))))
    ));

    personnel.addAll(List.of(
        new Personnel(1L, "customer-1", "openid-customer-1", "", "普通客户", "CUSTOMER", "13800000000", "启用",
            "首次登录自动建档的客户身份。", "2026-06-03 09:20", "2026-06-03 09:20"),
        new Personnel(2L, "sales-1", "openid-sales-1", "", "王销售", "SALES", "13900000001", "启用",
            "后台授权为销售，只能查看自己的报价。", "2026-06-03 09:30", "2026-06-03 09:30"),
        new Personnel(3L, "engineer-1", "openid-engineer-1", "", "技术工程师", "ENGINEER", "13900000002", "启用",
            "后台授权为技术工程师，可查看全部报价。", "2026-06-02 18:10", "2026-06-02 18:10"),
        new Personnel(4L, "admin-1", "openid-admin-1", "", "管理员", "ADMIN", "13900000003", "启用",
            "系统管理员，可管理人员权限和询价分配。", "2026-06-01 08:40", "2026-06-01 08:40")
    ));
  }

  public Personnel loginWithWechatCode(String code) {
    WechatIdentity identity = wechatIdentity(code);
    String openId = identity.openId();
    String unionId = identity.unionId();
    Personnel existing = personnel.stream().filter(item -> item.openId().equals(openId)).findFirst().orElse(null);
    String now = DISPLAY_TIME.format(Instant.now());
    if (existing == null) {
      long id = personnelIds.incrementAndGet();
      Personnel created = new Personnel(id, "customer-" + id, openId, unionId, "微信用户" + id, "CUSTOMER", "", "启用",
          "首次微信登录自动建档，默认普通客户。", now, now);
      personnel.add(created);
      return created;
    }
    if (!"启用".equals(existing.status())) {
      throw new IllegalArgumentException("该微信用户已被禁用，请联系管理员。");
    }
    Personnel updated = new Personnel(existing.id(), existing.userId(), existing.openId(), firstText(existing.unionId(), unionId),
        existing.displayName(), existing.role(), existing.phone(), existing.status(), existing.remark(), existing.firstLoginAt(), now);
    replacePersonnel(updated);
    return updated;
  }

  public List<String> permissionsForRole(String role) {
    return switch (normalizeRole(role)) {
      case "ADMIN" -> List.of("QUOTE_CREATE", "QUOTE_VIEW_ALL", "INQUIRY_ASSIGN", "PERSONNEL_MANAGE", "AI_CHAT", "FAVORITE");
      case "ENGINEER" -> List.of("QUOTE_CREATE", "QUOTE_VIEW_ALL", "AI_CHAT", "FAVORITE");
      case "SALES" -> List.of("QUOTE_CREATE", "QUOTE_VIEW_OWN", "AI_CHAT", "FAVORITE");
      default -> List.of("FAVORITE", "AI_CHAT", "QUOTE_MESSAGE");
    };
  }

  public Inquiry createInquiry(Inquiry payload) {
    long id = inquiryIds.incrementAndGet();
    Inquiry inquiry = new Inquiry(
        id,
        payload.contactName(),
        payload.phone(),
        payload.company(),
        payload.projectName(),
        payload.projectLocation(),
        payload.requirement(),
        "NEW",
        Instant.now()
    );
    inquiries.add(inquiry);
    managedInquiries.add(new ManagedInquiry(id, payload.contactName(), payload.company(), payload.projectName(),
        "待分配", "", "未分配", DISPLAY_TIME.format(Instant.now())));
    return inquiry;
  }

  public List<ManagedInquiry> managedInquiries() {
    return List.copyOf(managedInquiries);
  }

  public PageResult<Inquiry> inquiryPage(int page, int size, String keyword) {
    List<Inquiry> filtered = inquiries.stream()
        .filter(inquiry -> keyword == null || keyword.isBlank()
            || (inquiry.contactName() + inquiry.phone() + inquiry.company() + inquiry.projectName()).contains(keyword))
        .toList();
    return page(filtered, page, size);
  }

  public ManagedInquiry assignInquiry(Long inquiryId, String salesId) {
    SalesUser sales = salesUsers().stream()
        .filter(user -> user.id().equals(salesId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("销售人员不存在"));
    for (int index = 0; index < managedInquiries.size(); index += 1) {
      ManagedInquiry inquiry = managedInquiries.get(index);
      if (inquiry.id().equals(inquiryId)) {
        ManagedInquiry updated = new ManagedInquiry(inquiry.id(), inquiry.contactName(), inquiry.company(), inquiry.projectName(),
            "已分配", sales.id(), sales.name(), inquiry.createdAt());
        managedInquiries.set(index, updated);
        return updated;
      }
    }
    throw new IllegalArgumentException("询价不存在");
  }

  public Quote createQuote(Map<String, Object> payload, String ownerId, String ownerName) {
    Map<String, Object> project = asMap(payload.get("project"));
    Map<String, Object> customer = asMap(payload.get("customer"));
    Map<String, Object> totals = asMap(payload.get("totals"));
    String projectName = firstText(project.get("name"), payload.get("projectName"), "未命名项目");
    String customerName = firstText(customer.get("company"), customer.get("name"), payload.get("customerName"), "未填写客户");
    BigDecimal discountRate = decimal(payload.get("discountRate"), BigDecimal.valueOf(100));
    BigDecimal totalAmount = decimal(totals.get("finalAmount"), BigDecimal.ZERO);
    List<QuoteItem> items = quoteItemsFromMiniProgram(payload);
    if (items.isEmpty()) {
      items = quoteItemsFromLegacy(payload);
    }
    if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
      totalAmount = items.stream().map(QuoteItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    return saveQuote(null, projectName, customerName, discountRate, totalAmount, statusCode(payload.get("status")), items, ownerId, ownerName);
  }

  public Quote quote(Long id) {
    return quotes.stream().filter(quote -> quote.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("报价不存在"));
  }

  public List<Quote> quotes() {
    return List.copyOf(quotes);
  }

  public List<QuoteSummary> adminQuotes() {
    return List.copyOf(quoteSummaries);
  }

  public Map<String, Object> quoteDetail(Long id) {
    Quote quote = quote(id);
    QuoteSummary summary = quoteSummaries.stream().filter(item -> item.id().equals(id)).findFirst()
        .orElse(new QuoteSummary(quote.id(), quote.quoteNo(), quote.projectName(), "未填写客户", "sales-1", "王销售",
            quote.totalAmount(), statusLabel(quote.status()), DISPLAY_TIME.format(quote.createdAt())));
    List<Map<String, Object>> rows = quote.items().stream()
        .map(item -> Map.<String, Object>of(
            "measurement", item.measurement(),
            "categoryName", categoryFromMeasurement(item.measurement()),
            "productName", item.productName(),
            "quantity", item.quantity(),
            "unitPrice", item.unitPrice(),
            "amount", item.amount()
        ))
        .toList();
    return Map.of(
        "summary", summary,
        "quote", quote,
        "items", rows,
        "wordPreview", Map.of(
            "fileName", quote.quoteNo() + ".docx",
            "title", quote.projectName() + "报价书",
            "templateName", "标准设备报价书",
            "rows", rows,
            "totalAmount", quote.totalAmount(),
            "footer", "本报价为系统自动生成版本，正式报价以双方确认文件为准。"
        )
    );
  }

  public PageResult<Quote> quotePage(int page, int size) {
    return page(quotes, page, size);
  }

  public String exportQuote(Long id) {
    Quote quote = quote(id);
    return "/mock-files/quotes/" + quote.quoteNo() + ".docx";
  }

  public Map<String, Object> sendQuote(Long id) {
    boolean found = false;
    for (int index = 0; index < quotes.size(); index += 1) {
      Quote quote = quotes.get(index);
      if (quote.id().equals(id)) {
        quotes.set(index, new Quote(quote.id(), quote.inquiryId(), quote.quoteNo(), quote.projectName(), quote.version(),
            quote.discountRate(), quote.totalAmount(), "SENT", quote.items(), quote.createdAt()));
        found = true;
        break;
      }
    }
    for (int index = 0; index < quoteSummaries.size(); index += 1) {
      QuoteSummary summary = quoteSummaries.get(index);
      if (summary.id().equals(id)) {
        quoteSummaries.set(index, new QuoteSummary(summary.id(), summary.quoteNo(), summary.projectName(), summary.customerName(),
            summary.ownerId(), summary.ownerName(), summary.totalAmount(), "已发送", summary.createdAt()));
        found = true;
        break;
      }
    }
    if (!found) throw new IllegalArgumentException("报价不存在");
    return Map.of("id", id, "status", "SENT", "statusText", "已发送", "sentAt", DISPLAY_TIME.format(Instant.now()), "message", "报价单已发送");
  }

  public List<ConfigTemplate> templates() {
    return List.copyOf(templates);
  }

  public ConfigTemplate saveTemplate(Long id, Map<String, Object> payload) {
    ConfigTemplate template = new ConfigTemplate(
        id == null ? templateIds.incrementAndGet() : id,
        firstText(payload.get("templateName"), payload.get("name"), "未命名模板"),
        firstText(payload.get("description"), "暂无说明"),
        measurements(payload.get("measurements"))
    );
    upsertTemplate(template);
    return template;
  }

  public ConfigTemplate updateTemplate(Long id, Map<String, Object> payload) {
    templates.stream().filter(template -> template.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
    return saveTemplate(id, payload);
  }

  public Map<String, Object> deleteTemplate(Long id) {
    templates.removeIf(template -> template.id().equals(id));
    return Map.of("deleted", true, "id", id);
  }

  public Map<String, Object> dashboard() {
    long pending = managedInquiries.stream().filter(item -> "待分配".equals(item.status())).count();
    long sent = quoteSummaries.stream().filter(item -> "已发送".equals(item.status())).count();
    return Map.of(
        "inquiryCount", managedInquiries.size(),
        "quoteCount", quoteSummaries.size(),
        "pendingInquiryCount", pending,
        "sentQuoteCount", sent,
        "browseCount", 128,
        "aiQuestionCount", 12,
        "recentInquiries", managedInquiries.stream().limit(5).toList(),
        "recentQuotes", quoteSummaries.stream().limit(5).toList()
    );
  }

  public MeDashboard meDashboard(String role, String userId) {
    List<QuoteSummary> visibleQuotes = meQuotes(role, userId);
    int inquiryCount = "ADMIN".equals(role) ? managedInquiries.size() : 0;
    return new MeDashboard(favorites().size(), quoteMessages().size(), 4, visibleQuotes.size(), inquiryCount);
  }

  public List<FavoriteProduct> favorites() {
    return List.of(
        new FavoriteProduct(1L, "智能位移传感器", "PS-DIS-300", "边坡、坝体和矿山结构位移连续监测资料。"),
        new FavoriteProduct(2L, "多通道数据采集仪", "PS-DAQ-16", "多测点接入、边缘缓存和远程配置资料。")
    );
  }

  public List<QuoteMessage> quoteMessages() {
    return List.of(
        new QuoteMessage(1L, "北山边坡自动化监测", new BigDecimal("69008.00"), "已发送", "2026-06-03 10:30"),
        new QuoteMessage(2L, "尾矿库雨量通信监测", new BigDecimal("14880.00"), "待查看", "2026-06-02 16:12")
    );
  }

  public List<QuoteSummary> meQuotes(String role, String userId) {
    if ("SALES".equals(role)) {
      return quoteSummaries.stream().filter(quote -> quote.ownerId().equals(userId)).toList();
    }
    if ("ENGINEER".equals(role) || "ADMIN".equals(role)) {
      return List.copyOf(quoteSummaries);
    }
    return List.of();
  }

  public List<SalesUser> salesUsers() {
    List<SalesUser> users = personnel.stream()
        .filter(item -> "SALES".equals(item.role()) && "启用".equals(item.status()))
        .map(item -> new SalesUser(item.userId(), item.displayName()))
        .toList();
    return users.isEmpty() ? List.of(new SalesUser("sales-1", "王销售")) : users;
  }

  public List<Personnel> personnel() {
    return List.copyOf(personnel);
  }

  public Personnel updatePersonnel(Long id, Map<String, Object> payload) {
    Personnel existing = personnel.stream().filter(item -> item.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("人员档案不存在"));
    Personnel updated = new Personnel(
        existing.id(),
        existing.userId(),
        existing.openId(),
        firstText(payload.get("unionId"), existing.unionId()),
        firstText(payload.get("displayName"), existing.displayName()),
        normalizeRole(firstText(payload.get("role"), existing.role())),
        firstText(payload.get("phone"), existing.phone()),
        normalizeStatus(firstText(payload.get("status"), existing.status())),
        firstText(payload.get("remark"), existing.remark()),
        existing.firstLoginAt(),
        existing.lastLoginAt()
    );
    replacePersonnel(updated);
    return updated;
  }

  public Personnel enablePersonnel(Long id) {
    return setPersonnelStatus(id, "启用");
  }

  public Personnel disablePersonnel(Long id) {
    return setPersonnelStatus(id, "禁用");
  }

  public record MeDashboard(int favoriteCount, int quoteMessageCount, int aiQuestionCount, int quoteCount, int inquiryCount) {
  }

  public record FavoriteProduct(Long id, String name, String model, String summary) {
  }

  public record QuoteMessage(Long id, String projectName, BigDecimal amount, String status, String sentAt) {
  }

  public record QuoteSummary(Long id, String quoteNo, String projectName, String customerName, String ownerId,
                             String ownerName, BigDecimal totalAmount, String status, String createdAt) {
  }

  public record ManagedInquiry(Long id, String contactName, String company, String projectName, String status,
                               String assignedSalesId, String assignedSalesName, String createdAt) {
  }

  public record SalesUser(String id, String name) {
  }

  public record Personnel(Long id, String userId, String openId, String unionId, String displayName, String role,
                          String phone, String status, String remark, String firstLoginAt, String lastLoginAt) {
  }

  private WechatIdentity wechatIdentity(String code) {
    String normalized = code == null || code.isBlank() ? "dev-code" : code;
    if ("dev-code".equals(normalized)) return new WechatIdentity("openid-dev-customer", "");
    if (normalized.startsWith("mock-openid-")) return new WechatIdentity(normalized, "");
    if (wechatConfigured()) {
      String response = code2Session(normalized);
      String openId = jsonField(response, "openid");
      if (openId.isBlank()) {
        String errorMessage = jsonField(response, "errmsg");
        throw new IllegalArgumentException(errorMessage.isBlank() ? "微信登录失败，未获取到 OpenID" : errorMessage);
      }
      return new WechatIdentity(openId, jsonField(response, "unionid"));
    }
    return new WechatIdentity("openid-" + Math.abs(normalized.hashCode()),
        normalized.startsWith("union-") ? "union-" + Math.abs(normalized.hashCode()) : "");
  }

  private Personnel setPersonnelStatus(Long id, String status) {
    Personnel existing = personnel.stream().filter(item -> item.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("人员档案不存在"));
    Personnel updated = new Personnel(existing.id(), existing.userId(), existing.openId(), existing.unionId(),
        existing.displayName(), existing.role(), existing.phone(), status, existing.remark(), existing.firstLoginAt(), existing.lastLoginAt());
    replacePersonnel(updated);
    return updated;
  }

  private void replacePersonnel(Personnel next) {
    for (int index = 0; index < personnel.size(); index += 1) {
      if (personnel.get(index).id().equals(next.id())) {
        personnel.set(index, next);
        return;
      }
    }
    personnel.add(next);
  }

  private boolean wechatConfigured() {
    return wechatAppId != null && !wechatAppId.isBlank() && wechatSecret != null && !wechatSecret.isBlank();
  }

  private String code2Session(String code) {
    try {
      String url = "https://api.weixin.qq.com/sns/jscode2session"
          + "?appid=" + URLEncoder.encode(wechatAppId, StandardCharsets.UTF_8)
          + "&secret=" + URLEncoder.encode(wechatSecret, StandardCharsets.UTF_8)
          + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
          + "&grant_type=authorization_code";
      HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
      return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    } catch (Exception ex) {
      throw new IllegalArgumentException("微信登录接口调用失败: " + ex.getMessage());
    }
  }

  private void seedQuote(Long id, String quoteNo, String projectName, String customerName, String ownerId, String ownerName,
                         BigDecimal totalAmount, String status, String createdAt) {
    List<QuoteItem> items = List.of(
        new QuoteItem("标准监测站 / 传感器", "智能位移传感器", 2, new BigDecimal("6800.00"), new BigDecimal("13600.00")),
        new QuoteItem("标准监测站 / 采集仪", "多通道数据采集仪", 1, new BigDecimal("12800.00"), new BigDecimal("12800.00")),
        new QuoteItem("标准监测站 / 通信", "工业 4G 通信网关", 1, new BigDecimal("3600.00"), new BigDecimal("3600.00"))
    );
    quotes.add(new Quote(id, null, quoteNo, projectName, 1, new BigDecimal("95"), totalAmount, status, items, Instant.now()));
    quoteSummaries.add(new QuoteSummary(id, quoteNo, projectName, customerName, ownerId, ownerName, totalAmount, statusLabel(status), createdAt));
  }

  private Quote saveQuote(Long inquiryId, String projectName, String customerName, BigDecimal discountRate,
                          BigDecimal totalAmount, String status, List<QuoteItem> items, String ownerId, String ownerName) {
    long id = quoteIds.incrementAndGet();
    String quoteNo = "Q" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replaceAll("[^0-9]", "").substring(0, 14) + "-" + id;
    Quote quote = new Quote(id, inquiryId, quoteNo, projectName, 1, discountRate,
        totalAmount.setScale(2, RoundingMode.HALF_UP), status, items, Instant.now());
    quotes.add(quote);
    quoteSummaries.add(new QuoteSummary(id, quoteNo, projectName, customerName, ownerId, ownerName,
        quote.totalAmount(), statusLabel(status), DISPLAY_TIME.format(Instant.now())));
    return quote;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object value) {
    if (value instanceof Map<?, ?> map) return (Map<String, Object>) map;
    return Map.of();
  }

  @SuppressWarnings("unchecked")
  private static List<Object> asList(Object value) {
    if (value instanceof List<?> list) return (List<Object>) list;
    return List.of();
  }

  private static List<QuoteItem> quoteItemsFromMiniProgram(Map<String, Object> payload) {
    List<QuoteItem> items = new ArrayList<>();
    for (Object testItemValue : asList(payload.get("testItems"))) {
      Map<String, Object> testItem = asMap(testItemValue);
      String measurementName = firstText(testItem.get("name"), "测项");
      int siteCount = Math.max(1, integer(testItem.get("siteCount"), 1));
      for (Object categoryValue : asList(testItem.get("categories"))) {
        Map<String, Object> category = asMap(categoryValue);
        String categoryName = firstText(category.get("categoryName"), "未分类");
        for (Object productValue : asList(category.get("selectedProducts"))) {
          Map<String, Object> product = asMap(productValue);
          String productName = firstText(product.get("name"), "未命名产品");
          int quantity = Math.max(0, integer(product.get("quantity"), 1));
          BigDecimal unitPrice = decimal(product.get("standardPrice"), BigDecimal.ZERO);
          BigDecimal amount = decimal(product.get("subtotal"), unitPrice.multiply(BigDecimal.valueOf(quantity)).multiply(BigDecimal.valueOf(siteCount)));
          if (quantity > 0) {
            items.add(new QuoteItem(measurementName + " / " + categoryName, productName, quantity, unitPrice, amount.setScale(2, RoundingMode.HALF_UP)));
          }
        }
      }
    }
    return items;
  }

  private static List<QuoteItem> quoteItemsFromLegacy(Map<String, Object> payload) {
    List<QuoteItem> items = new ArrayList<>();
    for (Object measurementValue : asList(payload.get("measurements"))) {
      Map<String, Object> measurement = asMap(measurementValue);
      String measurementName = firstText(measurement.get("name"), "测项");
      String productName = firstText(measurement.get("productName"), "未命名产品");
      int quantity = Math.max(0, integer(measurement.get("quantity"), 1));
      BigDecimal unitPrice = decimal(measurement.get("unitPrice"), BigDecimal.ZERO);
      BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
      if (quantity > 0) {
        items.add(new QuoteItem(measurementName, productName, quantity, unitPrice, amount));
      }
    }
    return items;
  }

  private static List<QuoteMeasurement> measurements(Object value) {
    if (value instanceof List<?> list && !list.isEmpty()) {
      List<QuoteMeasurement> result = new ArrayList<>();
      for (Object item : list) {
        Map<String, Object> row = asMap(item);
        result.add(new QuoteMeasurement(
            firstText(row.get("name"), "标准监测站"),
            firstText(row.get("categoryName"), "传感器"),
            firstText(row.get("productName"), "智能位移传感器"),
            Math.max(1, integer(row.get("quantity"), 1)),
            decimal(row.get("unitPrice"), BigDecimal.ONE)
        ));
      }
      return result;
    }
    return List.of(new QuoteMeasurement("标准监测站", "传感器", "智能位移传感器", 1, new BigDecimal("6800.00")));
  }

  private void upsertTemplate(ConfigTemplate template) {
    for (int index = 0; index < templates.size(); index += 1) {
      if (templates.get(index).id().equals(template.id())) {
        templates.set(index, template);
        return;
      }
    }
    templates.add(template);
  }

  private static String jsonField(String json, String field) {
    if (json == null || json.isBlank()) return "";
    String pattern = "\"" + field + "\"\\s*:\\s*\"";
    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern + "([^\"]*)\"").matcher(json);
    return matcher.find() ? matcher.group(1) : "";
  }

  private static String categoryFromMeasurement(String measurement) {
    if (measurement == null || !measurement.contains("/")) return "设备";
    return measurement.substring(measurement.lastIndexOf("/") + 1).trim();
  }

  private static String firstText(Object... values) {
    for (Object value : values) {
      if (value != null && !value.toString().isBlank()) return value.toString();
    }
    return "";
  }

  private static int integer(Object value, int fallback) {
    if (value instanceof Number number) return number.intValue();
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (Exception ignored) {
      return fallback;
    }
  }

  private static BigDecimal decimal(Object value, BigDecimal fallback) {
    if (value instanceof BigDecimal decimal) return decimal;
    if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
    try {
      return new BigDecimal(String.valueOf(value));
    } catch (Exception ignored) {
      return fallback;
    }
  }

  private static String statusCode(Object status) {
    String normalized = status == null ? "DRAFT" : status.toString().toUpperCase();
    if ("GENERATED".equals(normalized)) return "GENERATED";
    if ("SENT".equals(normalized)) return "SENT";
    return "DRAFT";
  }

  private static String statusLabel(String status) {
    return switch (status) {
      case "GENERATED" -> "已生成";
      case "SENT" -> "已发送";
      default -> "草稿";
    };
  }

  private static String normalizeRole(String role) {
    return switch (role == null ? "" : role.toUpperCase()) {
      case "SALES", "ENGINEER", "ADMIN" -> role.toUpperCase();
      default -> "CUSTOMER";
    };
  }

  private static String normalizeStatus(String status) {
    return "禁用".equals(status) ? "禁用" : "启用";
  }

  private static <T> PageResult<T> page(List<T> records, int page, int size) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.max(size, 1);
    int from = Math.min((safePage - 1) * safeSize, records.size());
    int to = Math.min(from + safeSize, records.size());
    return new PageResult<>(records.subList(from, to), records.size(), safePage, safeSize);
  }

  private record WechatIdentity(String openId, String unionId) {
  }
}

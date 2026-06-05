package com.productshow.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productshow.system.domain.Models.ConfigTemplate;
import com.productshow.system.domain.Models.Inquiry;
import com.productshow.system.domain.Models.PageResult;
import com.productshow.system.domain.Models.Quote;
import com.productshow.system.domain.Models.QuoteItem;
import com.productshow.system.domain.Models.QuoteMeasurement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesService {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final DateTimeFormatter DISPLAY_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final JdbcTemplate jdbc;
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Value("${wechat.appid:}")
  private String wechatAppId;

  @Value("${wechat.secret:}")
  private String wechatSecret;

  public SalesService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Personnel loginWithWechatCode(String code) {
    WechatIdentity identity = wechatIdentity(code);
    String now = DISPLAY_TIME.format(Instant.now());
    List<Personnel> matches = jdbc.query("SELECT * FROM personnel WHERE open_id = ? AND deleted = 0",
        (rs, rowNum) -> mapPersonnel(rs), identity.openId());
    if (matches.isEmpty()) {
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("user_id", "customer-" + Math.abs(identity.openId().hashCode()));
      values.put("open_id", identity.openId());
      values.put("union_id", identity.unionId());
      values.put("display_name", "微信用户");
      values.put("role", "CUSTOMER");
      values.put("phone", "");
      values.put("status", "启用");
      values.put("remark", "首次微信登录自动建档，默认普通客户。");
      values.put("first_login_at", now);
      values.put("last_login_at", now);
      Number id = new SimpleJdbcInsert(jdbc).withTableName("personnel")
          .usingGeneratedKeyColumns("id")
          .executeAndReturnKey(values);
      return personnelById(id.longValue());
    }
    Personnel existing = matches.get(0);
    if (!"启用".equals(existing.status())) {
      throw new IllegalArgumentException("该微信用户已被禁用，请联系管理员。");
    }
    jdbc.update("UPDATE personnel SET union_id = ?, last_login_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        firstText(existing.unionId(), identity.unionId()), now, existing.id());
    return personnelById(existing.id());
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
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("contact_name", payload.contactName());
    values.put("phone", payload.phone());
    values.put("company", payload.company());
    values.put("project_name", payload.projectName());
    values.put("project_location", payload.projectLocation());
    values.put("requirement", payload.requirement());
    values.put("status", "待分配");
    values.put("assigned_sales_id", "");
    values.put("assigned_sales_name", "未分配");
    Number id = new SimpleJdbcInsert(jdbc).withTableName("inquiry")
        .usingGeneratedKeyColumns("id")
        .executeAndReturnKey(values);
    return new Inquiry(id.longValue(), payload.contactName(), payload.phone(), payload.company(), payload.projectName(),
        payload.projectLocation(), payload.requirement(), "待分配", Instant.now());
  }

  public List<ManagedInquiry> managedInquiries() {
    return jdbc.query("SELECT * FROM inquiry WHERE deleted = 0 ORDER BY id DESC", (rs, rowNum) -> mapManagedInquiry(rs));
  }

  public PageResult<Inquiry> inquiryPage(int page, int size, String keyword) {
    List<Inquiry> records = jdbc.query("SELECT * FROM inquiry WHERE deleted = 0 ORDER BY id DESC", (rs, rowNum) -> mapInquiry(rs))
        .stream()
        .filter(inquiry -> keyword == null || keyword.isBlank()
            || (inquiry.contactName() + inquiry.phone() + inquiry.company() + inquiry.projectName()).contains(keyword))
        .toList();
    return page(records, page, size);
  }

  public ManagedInquiry assignInquiry(Long inquiryId, String salesId) {
    SalesUser sales = salesUsers().stream()
        .filter(user -> user.id().equals(salesId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("销售人员不存在"));
    int updated = jdbc.update("""
            UPDATE inquiry
            SET status = '已分配', assigned_sales_id = ?, assigned_sales_name = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted = 0
            """, sales.id(), sales.name(), inquiryId);
    if (updated == 0) throw new IllegalArgumentException("询价不存在");
    return managedInquiries().stream().filter(item -> item.id().equals(inquiryId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("询价不存在"));
  }

  public Quote createQuote(Map<String, Object> payload, String ownerId, String ownerName) {
    Map<String, Object> project = asMap(payload.get("project"));
    Map<String, Object> customer = asMap(payload.get("customer"));
    Map<String, Object> totals = asMap(payload.get("totals"));
    String projectName = firstText(project.get("name"), payload.get("projectName"), "未命名项目");
    String customerName = firstText(customer.get("company"), customer.get("name"), payload.get("customerName"), "未填写客户");
    BigDecimal discountRate = decimal(payload.get("discountRate"), BigDecimal.valueOf(100));
    List<QuoteItem> items = quoteItemsFromMiniProgram(payload);
    if (items.isEmpty()) items = quoteItemsFromLegacy(payload);
    BigDecimal totalAmount = decimal(totals.get("finalAmount"), BigDecimal.ZERO);
    if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
      totalAmount = items.stream().map(QuoteItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    String quoteNo = "Q" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(Instant.now())
        + "-" + Math.abs(System.nanoTime() % 10000);
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("inquiry_id", longValue(payload.get("inquiryId"), null));
    values.put("quote_no", quoteNo);
    values.put("project_name", projectName);
    values.put("customer_name", customerName);
    values.put("version", 1);
    values.put("discount_rate", discountRate);
    values.put("total_amount", totalAmount.setScale(2, RoundingMode.HALF_UP));
    values.put("status", statusCode(payload.get("status")));
    values.put("owner_id", ownerId == null || ownerId.isBlank() ? "sales-1" : ownerId);
    values.put("owner_name", ownerName == null || ownerName.isBlank() ? "王销售" : ownerName);
    Number id = new SimpleJdbcInsert(jdbc).withTableName("quotation")
        .usingGeneratedKeyColumns("id")
        .executeAndReturnKey(values);
    for (QuoteItem item : items) insertQuoteItem(id.longValue(), item);
    return quote(id.longValue());
  }

  public Quote quote(Long id) {
    List<Quote> records = jdbc.query("SELECT * FROM quotation WHERE id = ? AND deleted = 0",
        (rs, rowNum) -> mapQuote(rs), id);
    if (records.isEmpty()) throw new IllegalArgumentException("报价不存在");
    return records.get(0);
  }

  public List<Quote> quotes() {
    return jdbc.query("SELECT * FROM quotation WHERE deleted = 0 ORDER BY id DESC", (rs, rowNum) -> mapQuote(rs));
  }

  public PageResult<Quote> quotePage(int page, int size) {
    return page(quotes(), page, size);
  }

  public List<QuoteSummary> adminQuotes() {
    return jdbc.query("SELECT * FROM quotation WHERE deleted = 0 ORDER BY id DESC", (rs, rowNum) -> mapQuoteSummary(rs));
  }

  public Map<String, Object> quoteDetail(Long id) {
    Quote quote = quote(id);
    QuoteSummary summary = quoteSummary(id);
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

  public String exportQuote(Long id) {
    Quote quote = quote(id);
    return "/mock-files/quotes/" + quote.quoteNo() + ".docx";
  }

  public Map<String, Object> sendQuote(Long id) {
    quote(id);
    String sentAt = DISPLAY_TIME.format(Instant.now());
    jdbc.update("UPDATE quotation SET status = 'SENT', sent_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", sentAt, id);
    return Map.of("id", id, "status", "SENT", "statusText", "已发送", "sentAt", sentAt, "message", "报价单已发送");
  }

  public List<ConfigTemplate> templates() {
    return jdbc.query("SELECT * FROM config_template WHERE deleted = 0 ORDER BY id", (rs, rowNum) -> mapTemplate(rs));
  }

  public ConfigTemplate saveTemplate(Long id, Map<String, Object> payload) {
    String templateName = firstText(payload.get("templateName"), payload.get("name"), "未命名模板");
    String description = firstText(payload.get("description"), "暂无说明");
    String measurementsJson = json(measurements(payload.get("measurements")));
    if (id == null) {
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("template_name", templateName);
      values.put("description", description);
      values.put("measurements_json", measurementsJson);
      Number nextId = new SimpleJdbcInsert(jdbc).withTableName("config_template")
          .usingGeneratedKeyColumns("id")
          .executeAndReturnKey(values);
      return template(nextId.longValue());
    }
    template(id);
    jdbc.update("""
            UPDATE config_template
            SET template_name = ?, description = ?, measurements_json = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted = 0
            """, templateName, description, measurementsJson, id);
    return template(id);
  }

  public ConfigTemplate updateTemplate(Long id, Map<String, Object> payload) {
    return saveTemplate(id, payload);
  }

  public Map<String, Object> deleteTemplate(Long id) {
    jdbc.update("UPDATE config_template SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    return Map.of("deleted", true, "id", id);
  }

  public Map<String, Object> dashboard() {
    List<ManagedInquiry> inquiryList = managedInquiries();
    List<QuoteSummary> quoteList = adminQuotes();
    long pending = inquiryList.stream().filter(item -> "待分配".equals(item.status())).count();
    long sent = quoteList.stream().filter(item -> "已发送".equals(item.status())).count();
    return Map.of(
        "inquiryCount", inquiryList.size(),
        "quoteCount", quoteList.size(),
        "pendingInquiryCount", pending,
        "sentQuoteCount", sent,
        "browseCount", 128,
        "aiQuestionCount", questionCount(),
        "recentInquiries", inquiryList.stream().limit(5).toList(),
        "recentQuotes", quoteList.stream().limit(5).toList()
    );
  }

  public MeDashboard meDashboard(String role, String userId) {
    List<QuoteSummary> visibleQuotes = meQuotes(role, userId);
    int inquiryCount = "ADMIN".equals(normalizeRole(role)) ? managedInquiries().size() : 0;
    return new MeDashboard(favorites().size(), quoteMessages().size(), questionCount(), visibleQuotes.size(), inquiryCount);
  }

  public List<FavoriteProduct> favorites() {
    return jdbc.query("SELECT id, name, model, summary FROM product WHERE deleted = 0 ORDER BY id LIMIT 5",
        (rs, rowNum) -> new FavoriteProduct(rs.getLong("id"), rs.getString("name"), rs.getString("model"), rs.getString("summary")));
  }

  public List<QuoteMessage> quoteMessages() {
    return jdbc.query("SELECT id, project_name, total_amount, status, sent_at, created_at FROM quotation WHERE deleted = 0 ORDER BY id DESC LIMIT 5",
        (rs, rowNum) -> new QuoteMessage(
            rs.getLong("id"),
            rs.getString("project_name"),
            rs.getBigDecimal("total_amount"),
            statusLabel(rs.getString("status")),
            firstText(rs.getString("sent_at"), timestampText(rs, "created_at"))
        ));
  }

  public List<QuoteSummary> meQuotes(String role, String userId) {
    String normalized = normalizeRole(role);
    if ("SALES".equals(normalized)) {
      return jdbc.query("SELECT * FROM quotation WHERE deleted = 0 AND owner_id = ? ORDER BY id DESC",
          (rs, rowNum) -> mapQuoteSummary(rs), userId);
    }
    if ("ENGINEER".equals(normalized) || "ADMIN".equals(normalized)) return adminQuotes();
    return List.of();
  }

  public List<SalesUser> salesUsers() {
    List<SalesUser> users = jdbc.query("""
            SELECT user_id, display_name
            FROM personnel
            WHERE deleted = 0 AND role = 'SALES' AND status = '启用'
            ORDER BY id
            """, (rs, rowNum) -> new SalesUser(rs.getString("user_id"), rs.getString("display_name")));
    return users.isEmpty() ? List.of(new SalesUser("sales-1", "王销售")) : users;
  }

  public List<Personnel> personnel() {
    return jdbc.query("SELECT * FROM personnel WHERE deleted = 0 ORDER BY id", (rs, rowNum) -> mapPersonnel(rs));
  }

  public Personnel updatePersonnel(Long id, Map<String, Object> payload) {
    Personnel existing = personnelById(id);
    jdbc.update("""
            UPDATE personnel
            SET union_id = ?, display_name = ?, role = ?, phone = ?, status = ?, remark = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted = 0
            """,
        firstText(payload.get("unionId"), existing.unionId()),
        firstText(payload.get("displayName"), existing.displayName()),
        normalizeRole(firstText(payload.get("role"), existing.role())),
        firstText(payload.get("phone"), existing.phone()),
        normalizeStatus(firstText(payload.get("status"), existing.status())),
        firstText(payload.get("remark"), existing.remark()),
        id);
    return personnelById(id);
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

  private void insertQuoteItem(Long quoteId, QuoteItem item) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("quote_id", quoteId);
    values.put("measurement", item.measurement());
    values.put("product_name", item.productName());
    values.put("quantity", item.quantity());
    values.put("unit_price", item.unitPrice());
    values.put("amount", item.amount());
    new SimpleJdbcInsert(jdbc).withTableName("quotation_item").usingGeneratedKeyColumns("id").execute(values);
  }

  private Quote mapQuote(ResultSet rs) throws SQLException {
    Long id = rs.getLong("id");
    return new Quote(
        id,
        rs.getObject("inquiry_id") == null ? null : rs.getLong("inquiry_id"),
        rs.getString("quote_no"),
        rs.getString("project_name"),
        rs.getInt("version"),
        rs.getBigDecimal("discount_rate"),
        rs.getBigDecimal("total_amount"),
        rs.getString("status"),
        quoteItems(id),
        rs.getTimestamp("created_at").toInstant()
    );
  }

  private List<QuoteItem> quoteItems(Long quoteId) {
    return jdbc.query("""
            SELECT measurement, product_name, quantity, unit_price, amount
            FROM quotation_item
            WHERE quote_id = ? AND deleted = 0
            ORDER BY id
            """,
        (rs, rowNum) -> new QuoteItem(
            rs.getString("measurement"),
            rs.getString("product_name"),
            rs.getInt("quantity"),
            rs.getBigDecimal("unit_price"),
            rs.getBigDecimal("amount")
        ), quoteId);
  }

  private QuoteSummary quoteSummary(Long id) {
    List<QuoteSummary> records = jdbc.query("SELECT * FROM quotation WHERE id = ? AND deleted = 0",
        (rs, rowNum) -> mapQuoteSummary(rs), id);
    if (records.isEmpty()) throw new IllegalArgumentException("报价不存在");
    return records.get(0);
  }

  private QuoteSummary mapQuoteSummary(ResultSet rs) throws SQLException {
    return new QuoteSummary(
        rs.getLong("id"),
        rs.getString("quote_no"),
        rs.getString("project_name"),
        rs.getString("customer_name"),
        rs.getString("owner_id"),
        rs.getString("owner_name"),
        rs.getBigDecimal("total_amount"),
        statusLabel(rs.getString("status")),
        timestampText(rs, "created_at")
    );
  }

  private ConfigTemplate template(Long id) {
    List<ConfigTemplate> records = jdbc.query("SELECT * FROM config_template WHERE id = ? AND deleted = 0",
        (rs, rowNum) -> mapTemplate(rs), id);
    if (records.isEmpty()) throw new IllegalArgumentException("模板不存在");
    return records.get(0);
  }

  private ConfigTemplate mapTemplate(ResultSet rs) throws SQLException {
    return new ConfigTemplate(
        rs.getLong("id"),
        rs.getString("template_name"),
        rs.getString("description"),
        readMeasurements(rs.getString("measurements_json"))
    );
  }

  private Inquiry mapInquiry(ResultSet rs) throws SQLException {
    return new Inquiry(
        rs.getLong("id"),
        rs.getString("contact_name"),
        rs.getString("phone"),
        rs.getString("company"),
        rs.getString("project_name"),
        rs.getString("project_location"),
        rs.getString("requirement"),
        rs.getString("status"),
        rs.getTimestamp("created_at").toInstant()
    );
  }

  private ManagedInquiry mapManagedInquiry(ResultSet rs) throws SQLException {
    return new ManagedInquiry(
        rs.getLong("id"),
        rs.getString("contact_name"),
        rs.getString("company"),
        rs.getString("project_name"),
        rs.getString("status"),
        firstText(rs.getString("assigned_sales_id"), ""),
        firstText(rs.getString("assigned_sales_name"), "未分配"),
        timestampText(rs, "created_at")
    );
  }

  private Personnel personnelById(Long id) {
    List<Personnel> records = jdbc.query("SELECT * FROM personnel WHERE id = ? AND deleted = 0",
        (rs, rowNum) -> mapPersonnel(rs), id);
    if (records.isEmpty()) throw new IllegalArgumentException("人员档案不存在");
    return records.get(0);
  }

  private Personnel mapPersonnel(ResultSet rs) throws SQLException {
    return new Personnel(
        rs.getLong("id"),
        rs.getString("user_id"),
        rs.getString("open_id"),
        firstText(rs.getString("union_id"), ""),
        rs.getString("display_name"),
        rs.getString("role"),
        firstText(rs.getString("phone"), ""),
        rs.getString("status"),
        firstText(rs.getString("remark"), ""),
        firstText(rs.getString("first_login_at"), ""),
        firstText(rs.getString("last_login_at"), "")
    );
  }

  private Personnel setPersonnelStatus(Long id, String status) {
    personnelById(id);
    jdbc.update("UPDATE personnel SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", status, id);
    return personnelById(id);
  }

  private int questionCount() {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM ai_question_record", Integer.class);
    return count == null ? 0 : count;
  }

  private WechatIdentity wechatIdentity(String code) {
    String normalized = code == null || code.isBlank() ? "dev-code" : code;
    if ("dev-code".equals(normalized)) return new WechatIdentity("openid-dev-customer", "");
    if (normalized.startsWith("mock-openid-")) return new WechatIdentity(normalized.substring("mock-openid-".length()), "");
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
          BigDecimal amount = decimal(product.get("subtotal"),
              unitPrice.multiply(BigDecimal.valueOf(quantity)).multiply(BigDecimal.valueOf(siteCount)));
          if (quantity > 0) {
            items.add(new QuoteItem(measurementName + " / " + categoryName, productName, quantity,
                unitPrice, amount.setScale(2, RoundingMode.HALF_UP)));
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
      if (quantity > 0) items.add(new QuoteItem(measurementName, productName, quantity, unitPrice, amount));
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

  private static List<QuoteMeasurement> readMeasurements(String value) {
    if (value == null || value.isBlank()) return List.of();
    try {
      return JSON.readValue(value, new TypeReference<List<QuoteMeasurement>>() {
      });
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private static String json(Object value) {
    try {
      return JSON.writeValueAsString(value);
    } catch (Exception ignored) {
      return "[]";
    }
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

  private static Long longValue(Object value, Long fallback) {
    if (value instanceof Number number) return number.longValue();
    try {
      return Long.parseLong(String.valueOf(value));
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

  private static String timestampText(ResultSet rs, String column) throws SQLException {
    return rs.getTimestamp(column) == null ? "" : DISPLAY_TIME.format(rs.getTimestamp(column).toInstant());
  }

  private record WechatIdentity(String openId, String unionId) {
  }
}

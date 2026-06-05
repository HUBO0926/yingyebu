package com.productshow.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.productshow.system.domain.Models.Category;
import com.productshow.system.domain.Models.FileRecord;
import com.productshow.system.domain.Models.PageResult;
import com.productshow.system.domain.Models.Product;
import com.productshow.system.domain.Models.ProductRelation;
import com.productshow.system.domain.Models.ProjectCase;
import com.productshow.system.domain.Models.Solution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogService {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final JdbcTemplate jdbc;

  public CatalogService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, Object> home() {
    return Map.of(
        "banners", List.of(
            Map.of("title", "产品展示与智能报价系统", "subtitle", "面向监测工程的数字化销售平台"),
            Map.of("title", "AI 知识库助手", "subtitle", "产品、方案、案例和技术资料统一问答")
        ),
        "industries", List.of("边坡监测", "尾矿库监测", "水库大坝监测", "地质灾害监测", "智慧矿山"),
        "products", products(),
        "solutions", solutions(),
        "cases", cases()
    );
  }

  public List<Category> categories() {
    return jdbc.query(
        "SELECT id, name, sort_order FROM product_category WHERE deleted = 0 ORDER BY sort_order, id",
        (rs, rowNum) -> new Category(rs.getLong("id"), rs.getString("name"), rs.getInt("sort_order"))
    );
  }

  public Category saveCategory(Long id, Map<String, Object> payload) {
    String name = text(payload.get("name"), "").trim();
    if (name.isBlank()) throw new IllegalArgumentException("分类名称不能为空");
    int sortOrder = intValue(payload.get("sortOrder"), 99);
    if (id == null) {
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("name", name);
      values.put("sort_order", sortOrder);
      Number nextId = new SimpleJdbcInsert(jdbc).withTableName("product_category")
          .usingGeneratedKeyColumns("id")
          .executeAndReturnKey(values);
      return new Category(nextId.longValue(), name, sortOrder);
    }
    requireCategory(id);
    jdbc.update("UPDATE product_category SET name = ?, sort_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted = 0",
        name, sortOrder, id);
    return new Category(id, name, sortOrder);
  }

  public Category updateCategory(Long id, Map<String, Object> payload) {
    return saveCategory(id, payload);
  }

  public Map<String, Object> deleteCategory(Long id) {
    Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE category_id = ? AND deleted = 0", Integer.class, id);
    if (used != null && used > 0) throw new IllegalArgumentException("该分类下已有产品，不能删除");
    jdbc.update("UPDATE product_category SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    return Map.of("deleted", true, "id", id);
  }

  public List<Product> products() {
    return jdbc.query(productSql(""), this::mapProduct);
  }

  public PageResult<Product> productPage(int page, int size, String keyword) {
    List<Product> records = products().stream()
        .filter(product -> keyword == null || keyword.isBlank()
            || (product.name() + product.model() + product.categoryName()).contains(keyword))
        .toList();
    return page(records, page, size);
  }

  public Product product(Long id) {
    List<Product> records = jdbc.query(productSql("AND p.id = ?"), this::mapProduct, id);
    if (records.isEmpty()) throw new IllegalArgumentException("产品不存在");
    return records.get(0);
  }

  public Product saveProduct(Long id, Map<String, Object> payload) {
    Long categoryId = longValue(payload.get("categoryId"), 0L);
    Category category = requireCategory(categoryId);
    String name = text(payload.get("name"), "").trim();
    if (name.isBlank()) throw new IllegalArgumentException("产品名称不能为空");

    List<String> carouselImages = stringList(payload.get("carouselImages"));
    String imageUrl = text(payload.get("imageUrl"), carouselImages.isEmpty()
        ? "https://dummyimage.com/960x540/f0f5ff/1677ff&text=PRODUCT"
        : carouselImages.get(0));
    if (carouselImages.isEmpty()) carouselImages = List.of(imageUrl);

    Map<String, Object> values = productValues(category.id(), name, payload, imageUrl, carouselImages);
    if (id == null) {
      Number nextId = new SimpleJdbcInsert(jdbc).withTableName("product")
          .usingGeneratedKeyColumns("id")
          .executeAndReturnKey(values);
      return product(nextId.longValue());
    }
    product(id);
    jdbc.update("""
            UPDATE product
            SET category_id = ?, name = ?, model = ?, summary = ?, parameters_json = ?, image_url = ?, price = ?,
                carousel_images_json = ?, detail_html = ?, attachments_json = ?, uploaded_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted = 0
            """,
        category.id(), values.get("name"), values.get("model"), values.get("summary"), values.get("parameters_json"),
        values.get("image_url"), values.get("price"), values.get("carousel_images_json"), values.get("detail_html"),
        values.get("attachments_json"), values.get("uploaded_by"), id);
    return product(id);
  }

  public Product updateProduct(Long id, Map<String, Object> payload) {
    return saveProduct(id, payload);
  }

  public Map<String, Object> deleteProduct(Long id) {
    jdbc.update("UPDATE product SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    return Map.of("deleted", true, "id", id);
  }

  public List<ProductRelation> relations(Long productId) {
    Product current = product(productId);
    return products().stream()
        .filter(product -> !product.id().equals(productId) && product.categoryId().compareTo(current.categoryId()) != 0)
        .limit(3)
        .map(product -> new ProductRelation(current.id(), product.id(), product.name(), 1,
            "建议与当前产品组合用于完整监测站配置。", true))
        .toList();
  }

  public List<Solution> solutions() {
    return jdbc.query("SELECT * FROM solution WHERE deleted = 0 ORDER BY id", this::mapSolution);
  }

  public Solution saveSolution(Long id, Map<String, Object> payload) {
    String title = text(payload.get("title"), "").trim();
    if (title.isBlank()) throw new IllegalArgumentException("方案标题不能为空");
    List<String> carouselImages = stringList(payload.get("carouselImages"));
    String imageUrl = text(payload.get("imageUrl"), carouselImages.isEmpty()
        ? "https://dummyimage.com/960x540/e8f2ff/1677ff&text=SOLUTION"
        : carouselImages.get(0));
    if (carouselImages.isEmpty()) carouselImages = List.of(imageUrl);
    Map<String, Object> values = solutionValues(title, payload, imageUrl, carouselImages);
    if (id == null) {
      Number nextId = new SimpleJdbcInsert(jdbc).withTableName("solution")
          .usingGeneratedKeyColumns("id")
          .executeAndReturnKey(values);
      return solution(nextId.longValue());
    }
    solution(id);
    jdbc.update("""
            UPDATE solution
            SET title = ?, industry = ?, summary = ?, architecture = ?, image_url = ?, recommended_product_ids_json = ?,
                carousel_images_json = ?, detail_html = ?, attachments_json = ?, uploaded_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted = 0
            """,
        values.get("title"), values.get("industry"), values.get("summary"), values.get("architecture"),
        values.get("image_url"), values.get("recommended_product_ids_json"), values.get("carousel_images_json"),
        values.get("detail_html"), values.get("attachments_json"), values.get("uploaded_by"), id);
    return solution(id);
  }

  public Solution updateSolution(Long id, Map<String, Object> payload) {
    return saveSolution(id, payload);
  }

  public Map<String, Object> deleteSolution(Long id) {
    jdbc.update("UPDATE solution SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    return Map.of("deleted", true, "id", id);
  }

  public List<ProjectCase> cases() {
    return jdbc.query("SELECT * FROM project_case WHERE deleted = 0 ORDER BY id", this::mapCase);
  }

  public ProjectCase saveCase(Long id, Map<String, Object> payload) {
    String name = text(payload.get("name"), "").trim();
    if (name.isBlank()) throw new IllegalArgumentException("案例名称不能为空");
    List<String> carouselImages = stringList(payload.get("carouselImages"));
    String imageUrl = text(payload.get("imageUrl"), carouselImages.isEmpty()
        ? "https://dummyimage.com/960x540/e8f2ff/1677ff&text=CASE"
        : carouselImages.get(0));
    if (carouselImages.isEmpty()) carouselImages = List.of(imageUrl);
    Map<String, Object> values = caseValues(name, payload, imageUrl, carouselImages);
    if (id == null) {
      Number nextId = new SimpleJdbcInsert(jdbc).withTableName("project_case")
          .usingGeneratedKeyColumns("id")
          .executeAndReturnKey(values);
      return projectCase(nextId.longValue());
    }
    projectCase(id);
    jdbc.update("""
            UPDATE project_case
            SET name = ?, location = ?, industry = ?, summary = ?, image_url = ?, carousel_images_json = ?,
                detail_html = ?, attachments_json = ?, uploaded_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND deleted = 0
            """,
        values.get("name"), values.get("location"), values.get("industry"), values.get("summary"), values.get("image_url"),
        values.get("carousel_images_json"), values.get("detail_html"), values.get("attachments_json"), values.get("uploaded_by"), id);
    return projectCase(id);
  }

  public ProjectCase updateCase(Long id, Map<String, Object> payload) {
    return saveCase(id, payload);
  }

  public Map<String, Object> deleteCase(Long id) {
    jdbc.update("UPDATE project_case SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    return Map.of("deleted", true, "id", id);
  }

  private Category requireCategory(Long id) {
    List<Category> matches = categories().stream().filter(category -> category.id().equals(id)).toList();
    if (matches.isEmpty()) throw new IllegalArgumentException("请选择有效产品分类");
    return matches.get(0);
  }

  private Solution solution(Long id) {
    List<Solution> matches = jdbc.query("SELECT * FROM solution WHERE id = ? AND deleted = 0", this::mapSolution, id);
    if (matches.isEmpty()) throw new IllegalArgumentException("方案不存在");
    return matches.get(0);
  }

  private ProjectCase projectCase(Long id) {
    List<ProjectCase> matches = jdbc.query("SELECT * FROM project_case WHERE id = ? AND deleted = 0", this::mapCase, id);
    if (matches.isEmpty()) throw new IllegalArgumentException("案例不存在");
    return matches.get(0);
  }

  private static String productSql(String clause) {
    return """
        SELECT p.*, c.name AS category_name
        FROM product p
        JOIN product_category c ON c.id = p.category_id AND c.deleted = 0
        WHERE p.deleted = 0
        """ + clause + " ORDER BY p.id";
  }

  private Product mapProduct(ResultSet rs, int rowNum) throws SQLException {
    return new Product(
        rs.getLong("id"),
        rs.getLong("category_id"),
        rs.getString("category_name"),
        rs.getString("name"),
        rs.getString("model"),
        rs.getString("summary"),
        readMap(rs.getString("parameters_json")),
        rs.getString("image_url"),
        rs.getBigDecimal("price"),
        readStringList(rs.getString("carousel_images_json")),
        rs.getString("detail_html"),
        readFileRecords(rs.getString("attachments_json")),
        rs.getString("uploaded_by"),
        timestampText(rs, "updated_at")
    );
  }

  private Solution mapSolution(ResultSet rs, int rowNum) throws SQLException {
    return new Solution(
        rs.getLong("id"),
        rs.getString("title"),
        rs.getString("industry"),
        rs.getString("summary"),
        rs.getString("architecture"),
        rs.getString("image_url"),
        readLongList(rs.getString("recommended_product_ids_json")),
        readStringList(rs.getString("carousel_images_json")),
        rs.getString("detail_html"),
        readFileRecords(rs.getString("attachments_json")),
        rs.getString("uploaded_by"),
        timestampText(rs, "updated_at")
    );
  }

  private ProjectCase mapCase(ResultSet rs, int rowNum) throws SQLException {
    return new ProjectCase(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getString("location"),
        rs.getString("industry"),
        rs.getString("summary"),
        rs.getString("image_url"),
        readStringList(rs.getString("carousel_images_json")),
        rs.getString("detail_html"),
        readFileRecords(rs.getString("attachments_json")),
        rs.getString("uploaded_by"),
        timestampText(rs, "updated_at")
    );
  }

  private static Map<String, Object> productValues(Long categoryId, String name, Map<String, Object> payload,
                                                   String imageUrl, List<String> carouselImages) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("category_id", categoryId);
    values.put("name", name);
    values.put("model", text(payload.get("model"), "MODEL"));
    values.put("summary", text(payload.get("summary"), "暂无简介"));
    values.put("parameters_json", json(parameters(payload.get("parameters"))));
    values.put("image_url", imageUrl);
    values.put("price", decimal(payload.get("price"), BigDecimal.ZERO));
    values.put("carousel_images_json", json(carouselImages));
    values.put("detail_html", text(payload.get("detailHtml"), defaultHtml(name)));
    values.put("attachments_json", json(fileRecords(payload.get("attachments"))));
    values.put("uploaded_by", text(payload.get("uploadedBy"), "管理员"));
    return values;
  }

  private static Map<String, Object> solutionValues(String title, Map<String, Object> payload,
                                                    String imageUrl, List<String> carouselImages) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("title", title);
    values.put("industry", text(payload.get("industry"), "边坡监测"));
    values.put("summary", text(payload.get("summary"), "暂无简介"));
    values.put("architecture", text(payload.get("architecture"), "现场设备 -> 数据采集 -> 通信回传 -> 平台分析"));
    values.put("image_url", imageUrl);
    values.put("recommended_product_ids_json", json(longList(payload.get("recommendedProductIds"))));
    values.put("carousel_images_json", json(carouselImages));
    values.put("detail_html", text(payload.get("detailHtml"), defaultHtml(title)));
    values.put("attachments_json", json(fileRecords(payload.get("attachments"))));
    values.put("uploaded_by", text(payload.get("uploadedBy"), "管理员"));
    return values;
  }

  private static Map<String, Object> caseValues(String name, Map<String, Object> payload,
                                                String imageUrl, List<String> carouselImages) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("name", name);
    values.put("location", text(payload.get("location"), "未填写地点"));
    values.put("industry", text(payload.get("industry"), "智慧矿山"));
    values.put("summary", text(payload.get("summary"), "暂无简介"));
    values.put("image_url", imageUrl);
    values.put("carousel_images_json", json(carouselImages));
    values.put("detail_html", text(payload.get("detailHtml"), defaultHtml(name)));
    values.put("attachments_json", json(fileRecords(payload.get("attachments"))));
    values.put("uploaded_by", text(payload.get("uploadedBy"), "管理员"));
    return values;
  }

  private static String defaultHtml(String title) {
    return "<h2>" + title + "</h2><p>资料内容由后台图文编辑器维护，可在小程序详情页展示。</p>";
  }

  private static Map<String, String> parameters(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, String> result = new LinkedHashMap<>();
      map.forEach((key, val) -> result.put(String.valueOf(key), String.valueOf(val)));
      return result;
    }
    return Map.of("说明", "待补充");
  }

  private static List<FileRecord> fileRecords(Object value) {
    if (!(value instanceof List<?> list)) return List.of();
    List<FileRecord> result = new ArrayList<>();
    for (Object item : list) {
      if (item instanceof FileRecord record) {
        result.add(record);
      } else if (item instanceof Map<?, ?> map) {
        result.add(new FileRecord(
            text(map.get("originalName"), text(map.get("name"), "附件")),
            text(map.get("objectKey"), ""),
            text(map.get("bucketName"), "local"),
            longValue(map.get("fileSize"), 0L),
            text(map.get("url"), ""),
            text(map.get("uploadedBy"), "管理员"),
            text(map.get("uploadedAt"), now()),
            text(map.get("module"), "common")
        ));
      }
    }
    return result;
  }

  private static List<String> stringList(Object value) {
    if (value instanceof List<?> list) return list.stream().map(String::valueOf).filter(item -> !item.isBlank()).toList();
    return List.of();
  }

  private static List<Long> longList(Object value) {
    if (value instanceof List<?> list) return list.stream().map(item -> longValue(item, 0L)).filter(item -> item > 0).toList();
    return List.of();
  }

  private static Map<String, String> readMap(String value) {
    if (value == null || value.isBlank()) return Map.of();
    try {
      return JSON.readValue(value, new TypeReference<Map<String, String>>() {
      });
    } catch (Exception ignored) {
      return Map.of();
    }
  }

  private static List<String> readStringList(String value) {
    if (value == null || value.isBlank()) return List.of();
    try {
      return JSON.readValue(value, new TypeReference<List<String>>() {
      });
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private static List<Long> readLongList(String value) {
    if (value == null || value.isBlank()) return List.of();
    try {
      return JSON.readValue(value, new TypeReference<List<Long>>() {
      });
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private static List<FileRecord> readFileRecords(String value) {
    if (value == null || value.isBlank()) return List.of();
    try {
      return JSON.readValue(value, new TypeReference<List<FileRecord>>() {
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

  private static String text(Object value, String fallback) {
    return value == null || value.toString().isBlank() ? fallback : value.toString();
  }

  private static int intValue(Object value, int fallback) {
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

  private static <T> PageResult<T> page(List<T> records, int page, int size) {
    int safePage = Math.max(page, 1);
    int safeSize = Math.max(size, 1);
    int from = Math.min((safePage - 1) * safeSize, records.size());
    int to = Math.min(from + safeSize, records.size());
    return new PageResult<>(records.subList(from, to), records.size(), safePage, safeSize);
  }

  private static String timestampText(ResultSet rs, String column) throws SQLException {
    return rs.getTimestamp(column) == null ? "" : rs.getTimestamp(column).toInstant().toString();
  }

  private static String now() {
    return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}

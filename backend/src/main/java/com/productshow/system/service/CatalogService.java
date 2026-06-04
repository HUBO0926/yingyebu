package com.productshow.system.service;

import com.productshow.system.domain.Models.Category;
import com.productshow.system.domain.Models.FileRecord;
import com.productshow.system.domain.Models.PageResult;
import com.productshow.system.domain.Models.Product;
import com.productshow.system.domain.Models.ProductRelation;
import com.productshow.system.domain.Models.ProjectCase;
import com.productshow.system.domain.Models.Solution;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CatalogService {
  private final AtomicLong categoryIds = new AtomicLong(20);
  private final AtomicLong productIds = new AtomicLong(20);
  private final AtomicLong solutionIds = new AtomicLong(20);
  private final AtomicLong caseIds = new AtomicLong(20);

  private final CopyOnWriteArrayList<Category> categories = new CopyOnWriteArrayList<>(List.of(
      new Category(1L, "传感器", 10),
      new Category(2L, "采集仪", 20),
      new Category(3L, "通信", 30),
      new Category(4L, "安装附件", 40),
      new Category(5L, "供电系统", 50),
      new Category(6L, "安装耗材", 60)
  ));

  private final CopyOnWriteArrayList<Product> products = new CopyOnWriteArrayList<>(List.of(
      product(1L, 1L, "传感器", "智能位移传感器", "PS-DIS-300", "用于边坡、坝体和矿山结构位移连续监测。",
          Map.of("量程", "300mm", "精度", "0.1mm", "防护", "IP67"),
          "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=900&q=80",
          "6800.00", "管理员"),
      product(2L, 1L, "传感器", "翻斗式雨量传感器", "PS-RAIN-01", "采集现场降雨数据，用于预警阈值判断和日报分析。",
          Map.of("分辨率", "0.2mm", "输出", "RS485", "防护", "IP65"),
          "https://dummyimage.com/960x540/edf7f1/16835b&text=PS-RAIN-01",
          "1800.00", "管理员"),
      product(3L, 2L, "采集仪", "多通道数据采集仪", "PS-DAQ-16", "支持多测点接入、边缘缓存和远程配置。",
          Map.of("通道", "16", "通信", "4G / Ethernet", "供电", "DC12V"),
          "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=80",
          "12800.00", "管理员"),
      product(4L, 2L, "采集仪", "边缘计算采集终端", "PS-EDGE-8", "适合小型监测站的数据采集、本地缓存和边缘计算。",
          Map.of("通道", "8", "存储", "32GB", "协议", "MQTT / HTTP"),
          "https://dummyimage.com/960x540/f7f0ff/722ed1&text=PS-EDGE-8",
          "9800.00", "管理员"),
      product(5L, 3L, "通信", "工业 4G 通信网关", "PS-GW-4G", "面向野外项目的数据回传和设备远程维护。",
          Map.of("网络", "4G Cat.4", "接口", "RS485 / LAN", "温度", "-20~70C"),
          "https://images.unsplash.com/photo-1597852074816-d933c7d2b988?auto=format&fit=crop&w=900&q=80",
          "3600.00", "管理员"),
      product(6L, 3L, "通信", "LoRa 无线网桥", "PS-LORA-BR", "用于山地和坝区短距离无线传输，降低布线成本。",
          Map.of("频段", "470MHz", "距离", "3km", "防护", "IP66"),
          "https://dummyimage.com/960x540/f6ffed/389e0d&text=PS-LORA",
          "2600.00", "管理员"),
      product(7L, 4L, "安装附件", "不锈钢安装支架", "PS-BRACKET-S", "用于传感器、采集箱和天线的现场固定。",
          Map.of("材质", "304", "高度", "1.2m", "安装", "地基 / 墙面"),
          "https://dummyimage.com/960x540/fff7e6/d46b08&text=BRACKET",
          "520.00", "管理员"),
      product(8L, 4L, "安装附件", "室外防护设备箱", "PS-BOX-500", "保护采集仪、电源和通信设备，适合野外长期部署。",
          Map.of("尺寸", "500x400x220", "防护", "IP65", "材质", "冷轧钢"),
          "https://dummyimage.com/960x540/fff1f0/c41d7f&text=PS-BOX",
          "880.00", "管理员"),
      product(9L, 5L, "供电系统", "太阳能供电箱", "PS-SOLAR-120", "适合无市电区域的监测站持续供电。",
          Map.of("功率", "120W", "电池", "80Ah", "防护", "IP65"),
          "https://images.unsplash.com/photo-1509391366360-2e959784a276?auto=format&fit=crop&w=900&q=80",
          "4200.00", "管理员"),
      product(10L, 5L, "供电系统", "备用锂电电源", "PS-BAT-50", "用于阴雨天或市电中断时的监测站备用供电。",
          Map.of("容量", "50Ah", "输出", "12V", "寿命", "2000 cycles"),
          "https://dummyimage.com/960x540/f9f0ff/531dab&text=BATTERY",
          "2300.00", "管理员"),
      product(11L, 6L, "安装耗材", "防水接头与线缆包", "PS-CABLE-KIT", "用于传感器到采集仪的现场接线和防水处理。",
          Map.of("线缆", "100m", "接头", "20pcs", "防护", "IP67"),
          "https://dummyimage.com/960x540/f0f5ff/1d39c4&text=CABLE",
          "960.00", "管理员"),
      product(12L, 6L, "安装耗材", "基础安装耗材包", "PS-INSTALL-PACK", "覆盖单个监测站的常用安装耗材和标识材料。",
          Map.of("含量", "螺栓 / 胶带 / 标签", "适用", "1 station", "类型", "通用"),
          "https://dummyimage.com/960x540/f5f5f5/595959&text=INSTALL",
          "680.00", "管理员")
  ));

  private final CopyOnWriteArrayList<Solution> solutions = new CopyOnWriteArrayList<>(List.of(
      solution(1L, "边坡自动化监测方案", "边坡监测", "位移、雨量、视频和采集网关组合，适合施工期与运营期监测。",
          "传感器层 -> 采集层 -> 通信层 -> 监测平台 -> 告警通知",
          "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80",
          List.of(1L, 3L, 5L, 9L), "管理员"),
      solution(2L, "尾矿库安全监测方案", "尾矿库监测", "围绕坝体位移、浸润线、库水位与视频巡检形成闭环。",
          "现场监测站 -> 工业网关 -> 云平台 -> 安全看板",
          "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80",
          List.of(1L, 3L, 9L, 11L), "管理员"),
      solution(3L, "水库大坝监测方案", "水库大坝监测", "支持渗压、扬压力、变形和环境量多源数据采集。",
          "监测仪器 -> 数据采集 -> 专线/4G -> 数据中心",
          "https://images.unsplash.com/photo-1437482078695-73f5ca6c96e2?auto=format&fit=crop&w=1200&q=80",
          List.of(2L, 3L, 5L), "管理员")
  ));

  private final CopyOnWriteArrayList<ProjectCase> cases = new CopyOnWriteArrayList<>(List.of(
      projectCase(1L, "华北矿区边坡监测项目", "河北 唐山", "智慧矿山", "部署 48 个监测点，形成自动化预警和日报分析。",
          "https://images.unsplash.com/photo-1473649085228-583485e6e4d7?auto=format&fit=crop&w=1200&q=80", "管理员"),
      projectCase(2L, "西南水库大坝安全监测", "云南 昆明", "水库大坝监测", "完成渗压、位移、雨量和视频联动监测。",
          "https://images.unsplash.com/photo-1437482078695-73f5ca6c96e2?auto=format&fit=crop&w=1200&q=80", "管理员"),
      projectCase(3L, "山区地质灾害预警示范点", "四川 雅安", "地质灾害监测", "实现雨量阈值和位移趋势联合预警。",
          "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80", "管理员")
  ));

  public Map<String, Object> home() {
    return Map.of(
        "banners", List.of(
            Map.of("title", "产品展示与智能报价系统", "subtitle", "面向监测工程的数字化销售平台"),
            Map.of("title", "AI 知识库助手", "subtitle", "产品、方案、技术资料统一问答")
        ),
        "industries", List.of("边坡监测", "尾矿库监测", "水库大坝监测", "地质灾害监测", "智慧矿山"),
        "products", products(),
        "solutions", solutions(),
        "cases", cases()
    );
  }

  public List<Category> categories() {
    return categories.stream().sorted((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder())).toList();
  }

  public Category saveCategory(Long id, Map<String, Object> payload) {
    String name = text(payload.get("name"), "").trim();
    if (name.isBlank()) throw new IllegalArgumentException("分类名称不能为空");
    Category category = new Category(
        id == null ? categoryIds.incrementAndGet() : id,
        name,
        intValue(payload.get("sortOrder"), 99)
    );
    upsert(categories, category, Category::id);
    return category;
  }

  public Category updateCategory(Long id, Map<String, Object> payload) {
    categories.stream().filter(category -> category.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("分类不存在"));
    return saveCategory(id, payload);
  }

  public Map<String, Object> deleteCategory(Long id) {
    boolean used = products.stream().anyMatch(product -> product.categoryId().equals(id));
    if (used) {
      throw new IllegalArgumentException("该分类下已有产品，不能删除");
    }
    categories.removeIf(category -> category.id().equals(id));
    return Map.of("deleted", true, "id", id);
  }

  public List<Product> products() {
    return List.copyOf(products);
  }

  public PageResult<Product> productPage(int page, int size, String keyword) {
    List<Product> filtered = products.stream()
        .filter(product -> keyword == null || keyword.isBlank()
            || (product.name() + product.model() + product.categoryName()).contains(keyword))
        .toList();
    return page(filtered, page, size);
  }

  public Product product(Long id) {
    return products.stream().filter(product -> product.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("产品不存在"));
  }

  public Product saveProduct(Long id, Map<String, Object> payload) {
    Long categoryId = longValue(payload.get("categoryId"), 0L);
    Category category = categories.stream()
        .filter(item -> item.id().equals(categoryId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("请选择有效产品分类"));
    List<String> carouselImages = stringList(payload.get("carouselImages"));
    String imageUrl = text(payload.get("imageUrl"), carouselImages.isEmpty()
        ? "https://dummyimage.com/960x540/f0f5ff/1677ff&text=PRODUCT"
        : carouselImages.get(0));
    if (carouselImages.isEmpty()) carouselImages = List.of(imageUrl);
    String name = text(payload.get("name"), "").trim();
    if (name.isBlank()) throw new IllegalArgumentException("产品名称不能为空");
    Product product = new Product(
        id == null ? productIds.incrementAndGet() : id,
        category.id(),
        category.name(),
        name,
        text(payload.get("model"), "MODEL"),
        text(payload.get("summary"), "暂无简介"),
        parameters(payload.get("parameters")),
        imageUrl,
        decimal(payload.get("price"), BigDecimal.ZERO),
        carouselImages,
        text(payload.get("detailHtml"), defaultProductHtml(name)),
        fileRecords(payload.get("attachments")),
        text(payload.get("uploadedBy"), "管理员"),
        now()
    );
    upsert(products, product, Product::id);
    return product;
  }

  public Product updateProduct(Long id, Map<String, Object> payload) {
    product(id);
    return saveProduct(id, payload);
  }

  public Map<String, Object> deleteProduct(Long id) {
    products.removeIf(product -> product.id().equals(id));
    return Map.of("deleted", true, "id", id);
  }

  public List<ProductRelation> relations(Long productId) {
    return switch (productId.intValue()) {
      case 1 -> List.of(
          new ProductRelation(1L, 3L, "多通道数据采集仪", 1, "位移传感器需要采集仪接入测点数据。", true),
          new ProductRelation(1L, 5L, "工业 4G 通信网关", 1, "野外项目建议配置通信网关。", true)
      );
      case 3 -> List.of(new ProductRelation(3L, 9L, "太阳能供电箱", 1, "无市电点位建议配置太阳能供电箱。", false));
      default -> List.of();
    };
  }

  public List<Solution> solutions() {
    return List.copyOf(solutions);
  }

  public Solution saveSolution(Long id, Map<String, Object> payload) {
    List<String> carouselImages = stringList(payload.get("carouselImages"));
    String imageUrl = text(payload.get("imageUrl"), carouselImages.isEmpty()
        ? "https://dummyimage.com/960x540/e8f2ff/1677ff&text=SOLUTION"
        : carouselImages.get(0));
    if (carouselImages.isEmpty()) carouselImages = List.of(imageUrl);
    String title = text(payload.get("title"), "未命名方案");
    Solution solution = new Solution(
        id == null ? solutionIds.incrementAndGet() : id,
        title,
        text(payload.get("industry"), "边坡监测"),
        text(payload.get("summary"), "暂无简介"),
        text(payload.get("architecture"), "现场设备 -> 数据采集 -> 通信回传 -> 平台分析"),
        imageUrl,
        longList(payload.get("recommendedProductIds")),
        carouselImages,
        text(payload.get("detailHtml"), defaultSolutionHtml(title)),
        fileRecords(payload.get("attachments")),
        text(payload.get("uploadedBy"), "管理员"),
        now()
    );
    upsert(solutions, solution, Solution::id);
    return solution;
  }

  public Solution updateSolution(Long id, Map<String, Object> payload) {
    solutions.stream().filter(solution -> solution.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("方案不存在"));
    return saveSolution(id, payload);
  }

  public Map<String, Object> deleteSolution(Long id) {
    solutions.removeIf(solution -> solution.id().equals(id));
    return Map.of("deleted", true, "id", id);
  }

  public List<ProjectCase> cases() {
    return List.copyOf(cases);
  }

  public ProjectCase saveCase(Long id, Map<String, Object> payload) {
    List<String> carouselImages = stringList(payload.get("carouselImages"));
    String imageUrl = text(payload.get("imageUrl"), carouselImages.isEmpty()
        ? "https://dummyimage.com/960x540/e8f2ff/1677ff&text=CASE"
        : carouselImages.get(0));
    if (carouselImages.isEmpty()) carouselImages = List.of(imageUrl);
    String name = text(payload.get("name"), "未命名案例");
    ProjectCase item = new ProjectCase(
        id == null ? caseIds.incrementAndGet() : id,
        name,
        text(payload.get("location"), "未填写地点"),
        text(payload.get("industry"), "智慧矿山"),
        text(payload.get("summary"), "暂无简介"),
        imageUrl,
        carouselImages,
        text(payload.get("detailHtml"), defaultCaseHtml(name)),
        fileRecords(payload.get("attachments")),
        text(payload.get("uploadedBy"), "管理员"),
        now()
    );
    upsert(cases, item, ProjectCase::id);
    return item;
  }

  public ProjectCase updateCase(Long id, Map<String, Object> payload) {
    cases.stream().filter(item -> item.id().equals(id)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("案例不存在"));
    return saveCase(id, payload);
  }

  public Map<String, Object> deleteCase(Long id) {
    cases.removeIf(item -> item.id().equals(id));
    return Map.of("deleted", true, "id", id);
  }

  private static Product product(Long id, Long categoryId, String categoryName, String name, String model, String summary,
                                 Map<String, String> parameters, String imageUrl, String price, String uploadedBy) {
    return new Product(id, categoryId, categoryName, name, model, summary, parameters, imageUrl, new BigDecimal(price),
        List.of(imageUrl), defaultProductHtml(name), sampleAttachments("产品资料", uploadedBy), uploadedBy, now());
  }

  private static Solution solution(Long id, String title, String industry, String summary, String architecture,
                                   String imageUrl, List<Long> productIds, String uploadedBy) {
    return new Solution(id, title, industry, summary, architecture, imageUrl, productIds, List.of(imageUrl),
        defaultSolutionHtml(title), sampleAttachments("方案资料", uploadedBy), uploadedBy, now());
  }

  private static ProjectCase projectCase(Long id, String name, String location, String industry, String summary,
                                         String imageUrl, String uploadedBy) {
    return new ProjectCase(id, name, location, industry, summary, imageUrl, List.of(imageUrl),
        defaultCaseHtml(name), sampleAttachments("案例资料", uploadedBy), uploadedBy, now());
  }

  private static List<FileRecord> sampleAttachments(String prefix, String uploadedBy) {
    return List.of(new FileRecord(prefix + ".pdf", "mock/" + prefix + ".pdf", "local", 204800,
        "/uploads/mock/" + prefix + ".pdf", uploadedBy, now(), "mock"));
  }

  private static String defaultProductHtml(String name) {
    return "<h2>" + name + "</h2><p>适用于工程监测现场长期运行，支持多点部署、远程采集和平台化管理。</p>"
        + "<ul><li>结构稳定，适合户外环境。</li><li>支持与采集仪、通信网关组合使用。</li><li>可作为报价模板中的标准设备项。</li></ul>";
  }

  private static String defaultSolutionHtml(String title) {
    return "<h2>" + title + "</h2><p>方案覆盖现场感知、数据采集、通信回传、平台分析和预警通知全流程。</p>"
        + "<table><tbody><tr><td>现场层</td><td>传感器、采集仪、供电和安装辅材</td></tr>"
        + "<tr><td>平台层</td><td>数据看板、趋势分析、告警联动</td></tr></tbody></table>";
  }

  private static String defaultCaseHtml(String name) {
    return "<h2>" + name + "</h2><p>项目完成后实现监测数据自动采集、异常预警和日报输出，提升现场巡检效率。</p>"
        + "<blockquote>该案例可作为同类项目报价和方案推荐的参考样板。</blockquote>";
  }

  private static Map<String, String> parameters(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<String, String> result = new LinkedHashMap<>();
      map.forEach((key, val) -> result.put(String.valueOf(key), String.valueOf(val)));
      return result;
    }
    return Map.of("说明", "待补充");
  }

  private static List<String> stringList(Object value) {
    if (value instanceof List<?> list) {
      List<String> result = new ArrayList<>();
      for (Object item : list) {
        if (item != null && !String.valueOf(item).isBlank()) result.add(String.valueOf(item));
      }
      return result;
    }
    return List.of();
  }

  private static List<FileRecord> fileRecords(Object value) {
    if (!(value instanceof List<?> list)) return List.of();
    List<FileRecord> result = new ArrayList<>();
    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
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

  private static List<Long> longList(Object value) {
    if (value instanceof List<?> list) {
      List<Long> result = new ArrayList<>();
      for (Object item : list) result.add(longValue(item, 0L));
      return result;
    }
    return List.of();
  }

  private static <T> void upsert(CopyOnWriteArrayList<T> records, T next, java.util.function.Function<T, Long> idGetter) {
    for (int index = 0; index < records.size(); index += 1) {
      if (idGetter.apply(records.get(index)).equals(idGetter.apply(next))) {
        records.set(index, next);
        return;
      }
    }
    records.add(next);
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

  private static String now() {
    return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}

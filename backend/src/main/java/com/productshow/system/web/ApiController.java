package com.productshow.system.web;

import cn.dev33.satoken.stp.StpUtil;
import com.productshow.system.ApiResponse;
import com.productshow.system.domain.Models.ChatRequest;
import com.productshow.system.domain.Models.Inquiry;
import com.productshow.system.domain.Models.LoginRequest;
import com.productshow.system.domain.Models.LoginResult;
import com.productshow.system.service.AiConfigService;
import com.productshow.system.service.AiPlaceholderService;
import com.productshow.system.service.AdminAuthService;
import com.productshow.system.service.CatalogService;
import com.productshow.system.service.FileStorageService;
import com.productshow.system.service.SalesService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final CatalogService catalogService;
  private final SalesService salesService;
  private final AiPlaceholderService aiPlaceholderService;
  private final FileStorageService fileStorageService;
  private final AiConfigService aiConfigService;
  private final AdminAuthService adminAuthService;

  public ApiController(CatalogService catalogService, SalesService salesService,
                       AiPlaceholderService aiPlaceholderService, FileStorageService fileStorageService,
                       AiConfigService aiConfigService, AdminAuthService adminAuthService) {
    this.catalogService = catalogService;
    this.salesService = salesService;
    this.aiPlaceholderService = aiPlaceholderService;
    this.fileStorageService = fileStorageService;
    this.aiConfigService = aiConfigService;
    this.adminAuthService = adminAuthService;
  }

  @GetMapping("/health")
  public ApiResponse<Map<String, String>> health() {
    return ApiResponse.ok(Map.of("status", "UP", "service", "product-show-backend"));
  }

  @PostMapping("/auth/wechat-login")
  public ApiResponse<LoginResult> login(@Valid @RequestBody LoginRequest request) {
    SalesService.Personnel person = salesService.loginWithWechatCode(request.code());
    StpUtil.login("wx:" + person.openId());
    StpUtil.getSession().set("role", person.role());
    StpUtil.getSession().set("userId", person.userId());
    StpUtil.getSession().set("openId", person.openId());
    StpUtil.getSession().set("displayName", person.displayName());
    return ApiResponse.ok(new LoginResult(
        StpUtil.getTokenValue(),
        person.openId(),
        person.unionId(),
        person.role(),
        person.displayName(),
        salesService.permissionsForRole(person.role())
    ));
  }

  @PostMapping("/admin/auth/qr")
  public ApiResponse<Object> createAdminLoginQr() {
    return ApiResponse.ok(adminAuthService.createSession());
  }

  @GetMapping("/admin/auth/qr/{loginId}")
  public ApiResponse<Object> adminLoginQrStatus(@PathVariable String loginId) {
    AdminAuthService.AdminLoginSession session = adminAuthService.getSession(loginId);
    if ("CONFIRMED".equals(session.status())) {
      StpUtil.login("admin:" + session.userId());
      StpUtil.getSession().set("role", session.role());
      StpUtil.getSession().set("userId", session.userId());
      StpUtil.getSession().set("openId", session.openId());
      StpUtil.getSession().set("displayName", session.displayName());
      return ApiResponse.ok(Map.of(
          "status", session.status(),
          "token", StpUtil.getTokenValue(),
          "userId", session.userId(),
          "openId", session.openId(),
          "role", session.role(),
          "displayName", session.displayName(),
          "permissions", session.permissions()
      ));
    }
    return ApiResponse.ok(Map.of(
        "loginId", session.loginId(),
        "qrText", session.qrText(),
        "status", session.status(),
        "expiresAt", session.expiresAt()
    ));
  }

  @PostMapping("/admin/auth/qr/{loginId}/confirm")
  public ApiResponse<Object> confirmAdminLoginQr(@PathVariable String loginId) {
    if (!StpUtil.isLogin()) throw new IllegalArgumentException("请先在小程序完成微信登录");
    AdminAuthService.AdminLoginSession session = adminAuthService.confirm(
        loginId,
        currentUserId(),
        currentDisplayName(),
        currentRole(),
        salesService.permissionsForRole(currentRole()),
        currentOpenId()
    );
    return ApiResponse.ok(Map.of(
        "status", session.status(),
        "role", session.role(),
        "displayName", session.displayName()
    ));
  }

  @GetMapping("/catalog/home")
  public ApiResponse<Object> home() {
    return ApiResponse.ok(catalogService.home());
  }

  @GetMapping("/categories")
  public ApiResponse<Object> categories() {
    return ApiResponse.ok(catalogService.categories());
  }

  @GetMapping("/admin/categories")
  public ApiResponse<Object> adminCategories() {
    return ApiResponse.ok(catalogService.categories());
  }

  @PostMapping("/admin/categories")
  public ApiResponse<Object> createCategory(@RequestBody Map<String, Object> body) {
    return ApiResponse.ok(catalogService.saveCategory(null, body));
  }

  @PutMapping("/admin/categories/{id}")
  public ApiResponse<Object> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    return ApiResponse.ok(catalogService.updateCategory(id, body));
  }

  @DeleteMapping("/admin/categories/{id}")
  public ApiResponse<Object> deleteCategory(@PathVariable Long id) {
    return ApiResponse.ok(catalogService.deleteCategory(id));
  }

  @GetMapping("/products")
  public ApiResponse<Object> products() {
    return ApiResponse.ok(catalogService.products());
  }

  @GetMapping("/products/page")
  public ApiResponse<Object> productPage(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(catalogService.productPage(page, size, keyword));
  }

  @GetMapping("/products/{id}")
  public ApiResponse<Object> product(@PathVariable Long id) {
    return ApiResponse.ok(catalogService.product(id));
  }

  @GetMapping("/products/{id}/relations")
  public ApiResponse<Object> productRelations(@PathVariable Long id) {
    return ApiResponse.ok(catalogService.relations(id));
  }

  @GetMapping("/solutions")
  public ApiResponse<Object> solutions() {
    return ApiResponse.ok(catalogService.solutions());
  }

  @GetMapping("/cases")
  public ApiResponse<Object> cases() {
    return ApiResponse.ok(catalogService.cases());
  }

  @PostMapping("/inquiries")
  public ApiResponse<Object> createInquiry(@Valid @RequestBody Inquiry inquiry) {
    return ApiResponse.ok(salesService.createInquiry(inquiry));
  }

  @GetMapping("/admin/dashboard")
  public ApiResponse<Object> dashboard() {
    return ApiResponse.ok(salesService.dashboard());
  }

  @GetMapping("/admin/products")
  public ApiResponse<Object> adminProducts() {
    return ApiResponse.ok(catalogService.products());
  }

  @PostMapping("/admin/products")
  public ApiResponse<Object> createProduct(@RequestBody Map<String, Object> body) {
    return ApiResponse.ok(catalogService.saveProduct(null, body));
  }

  @PutMapping("/admin/products/{id}")
  public ApiResponse<Object> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    return ApiResponse.ok(catalogService.updateProduct(id, body));
  }

  @DeleteMapping("/admin/products/{id}")
  public ApiResponse<Object> deleteProduct(@PathVariable Long id) {
    return ApiResponse.ok(catalogService.deleteProduct(id));
  }

  @GetMapping("/admin/solutions")
  public ApiResponse<Object> adminSolutions() {
    return ApiResponse.ok(catalogService.solutions());
  }

  @PostMapping("/admin/solutions")
  public ApiResponse<Object> createSolution(@RequestBody Map<String, Object> body) {
    return ApiResponse.ok(catalogService.saveSolution(null, body));
  }

  @PutMapping("/admin/solutions/{id}")
  public ApiResponse<Object> updateSolution(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    return ApiResponse.ok(catalogService.updateSolution(id, body));
  }

  @DeleteMapping("/admin/solutions/{id}")
  public ApiResponse<Object> deleteSolution(@PathVariable Long id) {
    return ApiResponse.ok(catalogService.deleteSolution(id));
  }

  @GetMapping("/admin/cases")
  public ApiResponse<Object> adminCases() {
    return ApiResponse.ok(catalogService.cases());
  }

  @PostMapping("/admin/cases")
  public ApiResponse<Object> createCase(@RequestBody Map<String, Object> body) {
    return ApiResponse.ok(catalogService.saveCase(null, body));
  }

  @PutMapping("/admin/cases/{id}")
  public ApiResponse<Object> updateCase(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    return ApiResponse.ok(catalogService.updateCase(id, body));
  }

  @DeleteMapping("/admin/cases/{id}")
  public ApiResponse<Object> deleteCase(@PathVariable Long id) {
    return ApiResponse.ok(catalogService.deleteCase(id));
  }

  @GetMapping("/admin/inquiries")
  public ApiResponse<Object> inquiries() {
    return ApiResponse.ok(salesService.managedInquiries());
  }

  @GetMapping("/admin/sales-users")
  public ApiResponse<Object> salesUsers() {
    return ApiResponse.ok(salesService.salesUsers());
  }

  @PostMapping("/admin/inquiries/{id}/assign")
  public ApiResponse<Object> assignInquiry(@PathVariable Long id, @RequestBody Map<String, String> body) {
    return ApiResponse.ok(salesService.assignInquiry(id, body.get("salesId")));
  }

  @GetMapping("/admin/inquiries/page")
  public ApiResponse<Object> inquiryPage(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(salesService.inquiryPage(page, size, keyword));
  }

  @PostMapping("/quotes")
  public ApiResponse<Object> createQuote(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(salesService.createQuote(request, currentUserId(), currentDisplayName()));
  }

  @GetMapping("/quotes")
  public ApiResponse<Object> quotes() {
    return ApiResponse.ok(salesService.quotes());
  }

  @GetMapping("/quotes/page")
  public ApiResponse<Object> quotePage(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.ok(salesService.quotePage(page, size));
  }

  @GetMapping("/quotes/{id}")
  public ApiResponse<Object> quote(@PathVariable Long id) {
    return ApiResponse.ok(salesService.quoteDetail(id));
  }

  @GetMapping("/admin/quotes")
  public ApiResponse<Object> adminQuotes() {
    return ApiResponse.ok(salesService.adminQuotes());
  }

  @GetMapping("/admin/quotes/{id}")
  public ApiResponse<Object> adminQuote(@PathVariable Long id) {
    return ApiResponse.ok(salesService.quoteDetail(id));
  }

  @PostMapping("/quotes/{id}/export")
  public ApiResponse<Object> exportQuote(@PathVariable Long id) {
    String url = salesService.exportQuote(id);
    return ApiResponse.ok(Map.of("fileName", url.substring(url.lastIndexOf('/') + 1), "url", url, "mock", true));
  }

  @PostMapping("/quotes/{id}/send")
  public ApiResponse<Object> sendQuote(@PathVariable Long id) {
    return ApiResponse.ok(salesService.sendQuote(id));
  }

  @GetMapping("/templates")
  public ApiResponse<Object> templates() {
    return ApiResponse.ok(salesService.templates());
  }

  @GetMapping("/admin/templates")
  public ApiResponse<Object> adminTemplates() {
    return ApiResponse.ok(salesService.templates());
  }

  @PostMapping("/admin/templates")
  public ApiResponse<Object> createTemplate(@RequestBody Map<String, Object> body) {
    return ApiResponse.ok(salesService.saveTemplate(null, body));
  }

  @PutMapping("/admin/templates/{id}")
  public ApiResponse<Object> updateTemplate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    return ApiResponse.ok(salesService.updateTemplate(id, body));
  }

  @DeleteMapping("/admin/templates/{id}")
  public ApiResponse<Object> deleteTemplate(@PathVariable Long id) {
    return ApiResponse.ok(salesService.deleteTemplate(id));
  }

  @GetMapping("/admin/personnel")
  public ApiResponse<Object> personnel() {
    return ApiResponse.ok(salesService.personnel());
  }

  @PutMapping("/admin/personnel/{id}")
  public ApiResponse<Object> updatePersonnel(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    return ApiResponse.ok(salesService.updatePersonnel(id, body));
  }

  @PostMapping("/admin/personnel/{id}/disable")
  public ApiResponse<Object> disablePersonnel(@PathVariable Long id) {
    return ApiResponse.ok(salesService.disablePersonnel(id));
  }

  @PostMapping("/admin/personnel/{id}/enable")
  public ApiResponse<Object> enablePersonnel(@PathVariable Long id) {
    return ApiResponse.ok(salesService.enablePersonnel(id));
  }

  @GetMapping("/admin/ai-configs")
  public ApiResponse<Object> aiConfigs() {
    return ApiResponse.ok(aiConfigService.list());
  }

  @PostMapping("/admin/ai-configs")
  public ApiResponse<Object> createAiConfig(@RequestBody Map<String, Object> body) {
    return ApiResponse.ok(aiConfigService.create(body));
  }

  @PutMapping("/admin/ai-configs/{id}")
  public ApiResponse<Object> updateAiConfig(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    return ApiResponse.ok(aiConfigService.update(id, body));
  }

  @PostMapping("/admin/ai-configs/{id}/enable")
  public ApiResponse<Object> enableAiConfig(@PathVariable Long id) {
    return ApiResponse.ok(aiConfigService.enable(id));
  }

  @PostMapping("/admin/ai-configs/test")
  public ApiResponse<Object> testAiConfig(@RequestBody Map<String, Object> body) {
    return ApiResponse.ok(aiConfigService.test(body));
  }

  @DeleteMapping("/admin/ai-configs/{id}/key")
  public ApiResponse<Object> clearAiConfigKey(@PathVariable Long id) {
    return ApiResponse.ok(aiConfigService.clearKey(id));
  }

  @GetMapping("/me/dashboard")
  public ApiResponse<Object> meDashboard() {
    return ApiResponse.ok(salesService.meDashboard(currentRole(), currentUserId()));
  }

  @GetMapping("/me/quotes")
  public ApiResponse<Object> meQuotes() {
    return ApiResponse.ok(salesService.meQuotes(currentRole(), currentUserId()));
  }

  @GetMapping("/me/favorites")
  public ApiResponse<Object> favorites() {
    return ApiResponse.ok(salesService.favorites());
  }

  @GetMapping("/me/quote-messages")
  public ApiResponse<Object> quoteMessages() {
    return ApiResponse.ok(salesService.quoteMessages());
  }

  @PostMapping("/files")
  public ApiResponse<Object> upload(@RequestPart MultipartFile file,
                                    @RequestParam(defaultValue = "common") String module,
                                    @RequestParam(defaultValue = "管理员") String uploadedBy) {
    return ApiResponse.ok(fileStorageService.store(file, module, uploadedBy));
  }

  @GetMapping("/ai/documents")
  public ApiResponse<Object> aiDocuments() {
    return ApiResponse.ok(aiPlaceholderService.documents());
  }

  @PostMapping("/ai/chat")
  public ApiResponse<Object> aiChat(@Valid @RequestBody ChatRequest request) {
    return ApiResponse.ok(aiPlaceholderService.chat(request.question()));
  }

  @GetMapping("/ai/question-records")
  public ApiResponse<Object> aiQuestionRecords() {
    return ApiResponse.ok(aiPlaceholderService.questionRecords());
  }

  private static String currentRole() {
    try {
      if (StpUtil.isLogin()) {
        Object role = StpUtil.getSession().get("role");
        return role == null ? "CUSTOMER" : role.toString();
      }
    } catch (Exception ignored) {
      return "CUSTOMER";
    }
    return "CUSTOMER";
  }

  private static String currentUserId() {
    try {
      if (StpUtil.isLogin()) {
        Object userId = StpUtil.getSession().get("userId");
        return userId == null ? "customer-1" : userId.toString();
      }
    } catch (Exception ignored) {
      return "customer-1";
    }
    return "customer-1";
  }

  private static String currentDisplayName() {
    try {
      if (StpUtil.isLogin()) {
        Object displayName = StpUtil.getSession().get("displayName");
        return displayName == null ? displayNameForRole(currentRole()) : displayName.toString();
      }
    } catch (Exception ignored) {
      return displayNameForRole(currentRole());
    }
    return displayNameForRole(currentRole());
  }

  private static String currentOpenId() {
    try {
      if (StpUtil.isLogin()) {
        Object openId = StpUtil.getSession().get("openId");
        return openId == null ? "" : openId.toString();
      }
    } catch (Exception ignored) {
      return "";
    }
    return "";
  }

  private static String displayNameForRole(String role) {
    return switch (role) {
      case "SALES" -> "王销售";
      case "ENGINEER" -> "技术工程师";
      case "ADMIN" -> "管理员";
      default -> "普通用户";
    };
  }
}

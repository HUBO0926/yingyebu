package com.productshow.system.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {
  private final Map<String, AdminLoginSession> sessions = new ConcurrentHashMap<>();

  public AdminLoginSession createSession() {
    String loginId = UUID.randomUUID().toString().replace("-", "");
    Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
    String qrText = "/pages/admin-login-confirm/admin-login-confirm?loginId=" + loginId;
    AdminLoginSession session = new AdminLoginSession(loginId, qrText, "PENDING", expiresAt, "", "", "", List.of(), "");
    sessions.put(loginId, session);
    return session;
  }

  public AdminLoginSession getSession(String loginId) {
    AdminLoginSession session = sessions.get(loginId);
    if (session == null) throw new IllegalArgumentException("登录二维码不存在或已失效");
    if (Instant.now().isAfter(session.expiresAt())) {
      AdminLoginSession expired = session.withStatus("EXPIRED");
      sessions.put(loginId, expired);
      return expired;
    }
    return session;
  }

  public AdminLoginSession confirm(String loginId, String userId, String displayName, String role,
                                   List<String> permissions, String openId) {
    AdminLoginSession session = getSession(loginId);
    if (!"PENDING".equals(session.status())) return session;
    if ("CUSTOMER".equals(role)) {
      AdminLoginSession denied = new AdminLoginSession(loginId, session.qrText(), "DENIED", session.expiresAt(),
          userId, displayName, role, permissions, openId);
      sessions.put(loginId, denied);
      throw new IllegalArgumentException("普通客户不能登录管理后台，请先由管理员授权为销售、技术工程师或管理员");
    }
    if (!List.of("SALES", "ENGINEER", "ADMIN").contains(role)) {
      throw new IllegalArgumentException("当前微信角色无后台登录权限");
    }
    AdminLoginSession confirmed = new AdminLoginSession(loginId, session.qrText(), "CONFIRMED", session.expiresAt(),
        userId, displayName, role, permissions, openId);
    sessions.put(loginId, confirmed);
    return confirmed;
  }

  public record AdminLoginSession(String loginId, String qrText, String status, Instant expiresAt, String userId,
                                  String displayName, String role, List<String> permissions, String openId) {
    AdminLoginSession withStatus(String status) {
      return new AdminLoginSession(loginId, qrText, status, expiresAt, userId, displayName, role, permissions, openId);
    }
  }
}

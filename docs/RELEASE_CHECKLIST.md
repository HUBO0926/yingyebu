# 发布前检查清单

## 服务器与域名

- 云服务器系统建议 Ubuntu 22.04/24.04，至少 2 核 4G、40G 系统盘。
- 安装 Git、Docker、Docker Compose。
- 安全组只开放：
  - `22` SSH
  - `80` HTTP 证书申请和跳转
  - `443` HTTPS
- 准备并解析域名：
  - `API_DOMAIN`：后端 API，例如 `api.example.com`
  - `ADMIN_DOMAIN`：管理后台，例如 `admin.example.com`
- 中国大陆服务器需要完成域名备案后，再配置微信小程序合法域名。

## 环境变量

- 复制 `.env.example` 为 `.env`。
- 必须修改默认值：
  - `MYSQL_PASSWORD`
  - `MYSQL_ROOT_PASSWORD`
  - `SA_TOKEN_SECRET`
  - `WECHAT_APPID`
  - `WECHAT_SECRET`
  - `API_DOMAIN`
  - `ADMIN_DOMAIN`
  - `PUBLIC_API_BASE_URL`
  - `ADMIN_WEB_BASE_URL`
- `.env` 不允许提交到 Git。

## Docker 部署验证

- 启动：
  - `docker compose up --build -d`
- 查看容器：
  - `docker compose ps`
- 查看后端日志：
  - `docker compose logs -f backend`
- 验证 API：
  - `curl https://API_DOMAIN/api/health`
- 验证后台：
  - 浏览器打开 `https://ADMIN_DOMAIN`
- 确认 MySQL volume：
  - 普通重启使用 `docker compose restart`
  - 不要在正式服务器执行 `docker compose down -v`

## 微信小程序体验版

- `miniprogram/app.js`：
  - `releaseApiBase` 必须替换为真实 HTTPS API。
  - `apiBase` 发布体验版时必须指向 `releaseApiBase`。
- 微信公众平台配置：
  - request 合法域名：`https://API_DOMAIN`
  - uploadFile 合法域名：`https://API_DOMAIN`
  - downloadFile 合法域名：`https://API_DOMAIN`
- 微信开发者工具：
  - 关闭“不校验合法域名、web-view、TLS 版本以及 HTTPS 证书”。
  - 上传体验版。
  - 添加体验成员并真机测试。

## 功能验收

- 小程序：
  - 微信登录。
  - 产品、方案、案例浏览。
  - 询价提交。
  - 我的页面角色展示。
  - 报价工具、报价预览、发送、下载提示。
  - 小恒 AI 助手占位问答。
- 管理后台：
  - 后台扫码登录。
  - 普通客户扫码被拒绝。
  - 管理员调整人员角色。
  - 产品分类新增、编辑、删除。
  - 新增产品选择分类并保存。
  - 上传轮播图、富文本图片和附件。
  - 询价分配。
  - 后台新建报价、预览、发送、下载。
  - AI 模型配置保存、启用、Key 脱敏。
- 数据持久化：
  - 新增产品、人员权限、询价、报价后执行 `docker compose restart backend`。
  - 重启后数据仍存在。

## 本地/CI 检查

- `cd admin-web && npm run build`
- `cd miniprogram && npm run lint`
- `cd backend && mvn test`
- GitHub Actions 三项检查必须通过后再部署体验版。

## 暂不作为体验版阻塞项

- 真实 Word docx 生成。
- AI 知识库向量入库和真实模型问答。
- 生产级监控告警。
- 自动化数据库备份脚本。

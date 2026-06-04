# 发布前检查清单

## 运行环境

- 服务器安装 Docker 和 Docker Compose，或安装 JDK17、Maven、Node.js 22。
- 复制 `.env.example` 为 `.env`，至少配置：
  - `SA_TOKEN_SECRET`
  - `WECHAT_APPID`
  - `WECHAT_SECRET`
  - `PUBLIC_API_BASE_URL`
  - `ADMIN_WEB_BASE_URL`
- 执行 `docker compose up --build`，确认：
  - 后端 API：`http://服务器:8080/api/health`
  - 管理后台：`http://服务器:5173`
  - AI 服务：`http://服务器:8000/health`

## 微信小程序

- 小程序后台配置合法域名：
  - request 合法域名：生产 API HTTPS 域名
  - uploadFile/downloadFile 合法域名：生产 API 或对象存储 HTTPS 域名
- `miniprogram/app.js` 发布前将 `apiBase` 切换为 `releaseApiBase`。
- 使用微信开发者工具上传体验版，真机测试：
  - 微信登录
  - 产品、方案、案例浏览
  - 询价提交
  - 我的页面角色展示
  - 报价工具、报价预览、发送与下载提示
  - 后台登录二维码确认页

## 管理后台

- 首次微信登录用户默认为普通客户，管理员需要在“系统设置 -> 人员权限管理”中调整为销售、技术工程师或管理员。
- 后台登录使用“小程序扫码确认”：
  - 普通客户扫码应拒绝登录
  - 销售可进入报价中心
  - 技术工程师可查看全部报价
  - 管理员可进入人员权限管理和询价分配

## 发布候选验证

- `cd admin-web && npm run build`
- `cd miniprogram && npm run lint`
- `cd backend && mvn test`
- `docker compose up --build`
- 浏览器验证管理后台主要页面：
  - 产品分类新增、编辑、删除
  - 新增产品选择分类并保存
  - 询价分配
  - 后台新建报价，多测项、六类设备、金额计算、预览、发送、下载
  - AI 模型配置保存和启用

## 上线提醒

- 当前后端仍是内存 mock 数据，服务重启后新增资料会丢失；正式生产需要迁移到 MySQL 持久化表。
- Word 报价书导出当前是 mock 文件入口，正式版需要接入真实 docx 生成。
- AI 知识库当前完成模型配置与占位问答，正式版需要接入向量入库、检索和问答链路。

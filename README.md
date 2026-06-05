# Product Show System

产品展示与智能报价系统 MVP，包含 Spring Boot API、Vue3 管理后台、微信小程序、FastAPI AI 服务占位和 Docker Compose 基础设施。

## 模块

- `backend/`：Spring Boot 3 API，Sa-Token，MySQL 持久化，Flyway 数据库迁移。
- `admin-web/`：Vue3、TypeScript、Vite、Element Plus、Pinia。
- `miniprogram/`：微信原生小程序，用于产品浏览、询价、报价工具和小恒 AI 助手入口。
- `ai-service/`：FastAPI AI 知识库服务占位，后续接入向量检索与真实模型调用。
- `deploy/`：部署辅助配置。MySQL 建表和种子数据已改由 Flyway 管理。
- `.github/workflows/`：CI 检查。

## 本地与服务器要求

完整联调需要：

- JDK 17
- Maven 3.9+
- Docker with Docker Compose
- Node.js 20+
- Python 3.12+

云服务器推荐 Ubuntu 22.04/24.04、2 核 4G 起步、系统盘 40G+。生产或体验版部署必须准备 HTTPS 域名，微信小程序不能使用 `localhost`、服务器 IP 或 HTTP。

## 快速启动

复制环境变量模板：

```bash
cp .env.example .env
```

编辑 `.env`，至少替换数据库密码、`SA_TOKEN_SECRET`、微信 AppID/Secret、`API_DOMAIN` 和 `ADMIN_DOMAIN`。

启动全套服务：

```bash
docker compose up --build -d
```

服务入口：

- API HTTPS：`https://你的 API 域名/api`
- 上传资源：`https://你的 API 域名/uploads`
- 管理后台：`https://你的后台域名`
- 本机调试 API：`http://127.0.0.1:8080/api`
- 本机调试后台：`http://127.0.0.1:5173`

## MySQL 持久化

- Flyway 迁移脚本：`backend/src/main/resources/db/migration/V1__init_product_show_schema.sql`
- 后端启动时会自动建表并写入种子数据。
- MySQL 数据保存在 Docker volume `mysql-data`，普通重启不会丢。
- 上传文件保存在 Docker volume `backend-uploads`，文件元数据写入 `file_record` 表。

如果开发环境曾经使用旧 SQL 初始化过 MySQL volume，首次切换到 Flyway 版本时可以清空旧测试数据：

```bash
docker compose down -v
docker compose up --build -d
```

正式服务器不要执行 `docker compose down -v`，它会删除数据库数据。

## 云服务器部署步骤

### Ubuntu 服务器

```bash
sudo apt update
sudo apt install -y git ca-certificates curl
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
```

重新登录服务器后：

```bash
git clone https://github.com/HUBO0926/yingyebu.git
cd yingyebu
cp .env.example .env
nano .env
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

验证：

```bash
curl https://你的 API 域名/api/health
```

### Windows 服务器

Windows 服务器推荐使用 WSL2 Ubuntu 承载 Docker Compose。仓库提供了一键部署脚本：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\deploy\windows\install.ps1
```

也可以生成独立 zip 安装包：

```powershell
.\deploy\windows\package.ps1
```

安装包输出到：

```text
dist/product-show-windows-installer.zip
```

## 微信小程序体验版

上线体验版前需要：

- 将 `miniprogram/app.js` 中 `releaseApiBase` 替换成真实 API 域名，例如 `https://api.example.com/api`。
- 微信公众平台配置合法域名：
  - request 合法域名：`https://你的 API 域名`
  - uploadFile 合法域名：`https://你的 API 域名`
  - downloadFile 合法域名：`https://你的 API 域名`
- 微信开发者工具关闭“不校验合法域名”后真机测试。
- 上传体验版，测试登录、产品浏览、询价、报价工具、报价预览、后台扫码登录和人员权限。

## 安全提醒

- 不要提交真实 `.env`、微信 Secret、数据库密码和大模型 Key。
- 云服务器安全组只需要开放 `22`、`80`、`443`。
- 不建议公网暴露 MySQL、Redis、MinIO、backend、admin-web 的内部端口。
- 正式试运行前设置 MySQL 自动备份，至少保留最近 7-14 天。

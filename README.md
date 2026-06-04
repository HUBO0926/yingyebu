# Product Show System

产品展示与智能报价系统 MVP，全栈骨架包含 Spring Boot API、Vue3 PC 后台、微信小程序和 Docker Compose 基础设施。AI 知识库第一阶段按 `AGENTS.md.docx` 只预留接口和记录表。

## Modules

- `backend/` - Spring Boot 3 API, Sa-Token, MyBatis Plus dependency baseline.
- `admin-web/` - Vue3, TypeScript, Vite, Element Plus, Pinia.
- `miniprogram/` - 微信原生小程序骨架。
- `ai-service/` - 后续阶段 FastAPI AI 知识库原型；第一阶段 Docker/CI 不强制启用。
- `deploy/` - MySQL 初始化脚本。
- `.github/workflows/` - CI 骨架。

## Local Prerequisites

当前工作区可生成代码并运行 Node/Python 检查。完整联调需要安装：

- JDK 17
- Docker Desktop with Docker Compose
- Node.js 20+
- Python 3.12+

## Quick Start

```bash
docker compose up --build
```

服务端口：

- Backend API: `http://localhost:8080/api`
- Admin Web: `http://localhost:5173`
- MinIO Console: `http://localhost:9001`

## Environment

Copy `.env.example` to `.env` and fill API keys if AI chat should call DeepSeek.

```bash
cp .env.example .env
```

第一阶段 AI chat 返回占位响应，并写入问答记录；暂不实现真实向量检索。

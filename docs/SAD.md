# 系统架构设计 SAD V1.0

## 总体架构

微信小程序和 PC 管理后台通过 Spring Boot API 访问业务能力。MySQL 存储业务数据，Redis 预留缓存与会话能力，MinIO 通过抽象服务承载图片、附件和 Word 文件。

AI 知识库第一阶段仅在后端保留记录表和接口占位；FastAPI、LangChain、Qdrant、DeepSeek 等能力作为后续阶段规划。

## 技术选型

- 后端：JDK17、Spring Boot 3.x、MyBatis Plus、Sa-Token、Redis、MySQL8、MinIO
- PC 后台：Vue3、TypeScript、Element Plus、Pinia、Axios
- 微信小程序：原生小程序、Vant Weapp
- 部署：Docker Compose，生产环境预留 Docker + Nginx

## 模块划分

- 用户模块：游客、销售、技术工程师、管理员
- 产品模块：分类管理、产品管理、附件、关联推荐
- 方案模块：方案管理、推荐产品、关联案例
- 案例模块：案例管理
- 询价模块：客户询价、状态流转
- 报价模块：报价单、测项、报价明细、折扣、版本管理
- 模板模块：标准配置模板
- 文件模块：图片、附件、Word 文件，统一走存储抽象
- AI 模块：文档记录、问答接口占位、问答记录

## 安全设计

- 认证：微信登录、Token 认证
- 权限：RBAC
- 数据：逻辑删除、操作日志预留


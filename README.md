# 未来小说系统 (Future Novel System)

## 🛠 技术栈
- **前端 (Frontend)**: Vue 3 + Element Plus + Vite
- **后端 (Backend)**: Java Spring Boot 3
- **数据库 (Database)**: 内存型存储 (Seeding 模式)

## 🚀 启动指南
1. 确保 Docker Desktop 已启动。
2. 在项目根目录执行：
   ```bash
   docker compose up --build -d
   ```
3. 等待容器构建并启动完成。

## 🔗 服务地址
- **前端页面**: [http://localhost:3000](http://localhost:3000)
- **后端 Swagger API 文档**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## 📁 项目文档 (Project Docs)
详细的项目开发与运维文档已持久化至 `./docs` 目录：
- [架构说明](./docs/architecture.md)
- [设计文档](./docs/design.md)
- [开发指南](./docs/development.md)
- [测试报告](./docs/testing.md)
- [用户手册](./docs/user_manual.md)
- [演进历史](./docs/planning/walkthrough_zh.md)

## ✨ 系统特性
- **沉浸式阅读**: 暖色护眼模式，Merriweather 衬线字体优化。
- **极致视觉**: 浅色毛玻璃美学设计，响应式布局。
- **全量容器化**: 一键部署，环境零依赖。
- **中文化支持**: 全系统中文界面与日期格式化。
- **章节创作与发布校验**: 作者可在小说详情页撰写章节并保存草稿；点击「标记为已发布」前，系统会检查标题非空、正文不少于 100 字（不含空白）、且无 `{{占位符}}`/`【待补充】`/`（此处省略）` 等未替换内容。校验在**前端预检 + Java 后端重复校验**两层执行，仅浏览器通过不代表可以发布，后端返回 400 时页面会逐条列出问题。

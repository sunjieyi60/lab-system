## 定时任务设计 Demo（基于参考文档）

目录说明：
- `sql/demo_schedule.sql`：建表 SQL + 若干 mock 数据。
- `frontend/ScheduleDemo.vue`：Vue 单页（Composition API），使用内置 mock 数据展示任务组、条件组、数据组、动作与告警配置，可模拟“条件评估 + 任务执行”流程。
- `config/sample-config.json`：与 SQL mock 对应的 JSON 配置示例，可供前端/后端调试。

使用建议：
1. 在本地 MySQL 执行 `sql/demo_schedule.sql`，得到 demo 数据。
2. 若需要对接真实后端，可将 `sample-config.json` 作为接口返回数据格式参考；前端组件中的 mock 数据可替换为接口请求。
3. 目前前端页面自带“模拟评估/执行”按钮，纯前端计算布尔结果与执行顺序，用于交互验证。

注意：
- Demo 仅为设计和交互参考，未集成到现有 Spring Boot 模块的编译流程，不会影响现有代码构建。
- 如需与后端真实联动，请在 service 模块新增接口并返回与 `sample-config.json` 同结构的数据。



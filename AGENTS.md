# ZionChat 开发协作说明（计划与约束）

## 总原则
- 目标：在 **不改变既有 UI 视觉风格** 的前提下，把现有“占位/半成品功能”补齐为可用版本，并与 HTML 参考实现保持一致。
- 所有新增/修改代码尽量避免引入中文输出（尤其是日志/异常信息），以降低 PowerShell/CI 编码导致的乱码风险。

## CI 工作流（强约束）
- 每次改代码后先运行：`.\gradlew.bat test`（本地 wrapper 目前是 placeholder，不做本地编译）。
- 仅当 **上一轮 GitHub Actions 构建为 success** 时才 bump 小版本号（`app/build.gradle` 的 `versionName` / `versionCode`）。
- 不在本地执行任何编译任务（不运行 assemble/bundle）。
- push 到 GitHub 触发自动编译，使用：`gh run watch <run-id> --exit-status` 监控结果；失败则按日志修复并重复流程。

## 分阶段实施计划
### Phase 1：修复 OAuth/模型相关的核心可用性（优先级最高）
- Codex OAuth：修复 `/responses` 调用 400（Unsupported content type）与请求头/请求体兼容性。
- Codex 模型列表：参考 `CLIProxyAPI` 的模型定义策略，保证 Codex 登录后能稳定看到可选模型（必要时加入“动态拉取 + 静态兜底”）。
- 统一 OAuth Provider：Codex / Antigravity / iFlow / Gemini CLI 的 OAuth 流程与存储字段，补齐 token 刷新策略（失败自动刷新并重试，成功后回写本地存储）。

### Phase 2：把 OAuth Provider 融入“内置供应商”体系（不额外入口）
- 在“Model services”页去掉单独的 “Connect with OAuth” 入口。
- 将 Codex / Antigravity / iFlow / Gemini CLI 作为内置供应商展示，右上角显示 “OAuth” 标签。
- 进入 Provider 配置页时：
  - OAuth 供应商显示“已登录/未登录”与“连接/断开”操作；
  - 不再要求选择类型/填写 API Key/URL；
  - OAuth 完成后页面形态与普通 provider 一致（图标/名称等），但配置项替换为 OAuth 状态模块。

### Phase 3：功能补齐（在 UI 不变的前提下）
- 工具调用与视觉能力：按 `CLIProxyAPI` 的请求/响应与工具链路处理补齐。
- 生图：在配置“Image model”后启用真实生图（已有雏形，继续完善 provider/模型匹配与错误提示）。
- 记忆（Memories）：参考 `rikkahub` 的实现补齐真实的记忆存储/读取/展示（页面已完成可先接入数据层）。
- 总结模型：支持配置“Conversation summary model”，并在对话过程中触发摘要更新（策略与频率需可控）。

### Phase 4：交互细节增强（谨慎推进）
- 底部弹层：支持拖拽关闭、键盘联动、点击按钮时先收起键盘再弹出面板等。
- 长按/点击灰色方块背景：全局统一取消无关 ripple/indication（仅保留必要的 press 动效）。
- 渐变/模糊：只在不影响组件层级的前提下调整范围与 zIndex，避免遮挡输入栏与工具按钮。

## 关键文件索引（便于定位）
- OAuth：`app/src/main/java/com/zionchat/app/data/OAuthClient.kt`
- API：`app/src/main/java/com/zionchat/app/data/ChatApiClient.kt`
- Provider/Model 数据结构：`app/src/main/java/com/zionchat/app/data/Models.kt`
- Provider 预设：`app/src/main/java/com/zionchat/app/data/ProviderPresets.kt`
- 主要对话页：`app/src/main/java/com/zionchat/app/ui/screens/ChatScreen.kt`
- Model services：`app/src/main/java/com/zionchat/app/ui/screens/ModelServicesScreen.kt`


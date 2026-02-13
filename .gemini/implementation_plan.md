# ChatScreen 重构 + Room 数据库迁移 - 实施计划

## 一、总览

### 目标
1. **拆分 ChatScreen.kt**（5752 行） → 多个文件，每个文件职责单一
2. **引入 ChatViewModel** → 将所有状态和业务逻辑从 Composable 移至 ViewModel
3. **引入 Room 数据库** → 替代 DataStore 存储 Conversations/Messages/Tags 等大量数据

### 约束
- 保持现有 UI 风格不变
- 保持所有现有功能不变
- 使用渐进式重构，确保每一步都可编译

---

## 二、文件拆分计划

### 当前 ChatScreen.kt 代码结构分析

| 行范围 | 功能模块 | 目标文件 |
|--------|---------|---------|
| 1-124 | 导入和常量 | 各文件分散 |
| 125-127 | PendingMessage, PendingImageAttachment | `ChatModels.kt` |
| 128-377 | ChatScreen 主入口 + 状态定义 | `ChatScreen.kt` (精简版) |
| 378-1670 | sendMessage() 函数 + 流式处理 | `ChatViewModel.kt` |
| 1670-2140 | 消息渲染 (UserMessageItem, AssistantMessageItem) | `MessageItems.kt` |
| 2140-2550 | 标签组件 (MessageTagRow, AppDevToolTagCard, McpTagDetailCard) | `MessageTags.kt` |
| 2550-2660 | AppDevTagDetailCard | `AppDevComponents.kt` |
| 2660-2920 | AppDevWorkspaceScreen, AppDevCodeSurface | `AppDevComponents.kt` |
| 2920-3000 | ToolDetailCodeCard, MessageOptionsDialog, DialogOption | `ChatDialogs.kt` |
| 3000-3350 | EmptyChatState, SidebarContent, SidebarMenuItem, TopNavBar, ActionButton | `ChatNavComponents.kt` |
| 3350-3700 | ToolMenuPanel, QuickActionButton, ToolListItem | `ToolMenuPanel.kt` |
| 3690-4015 | BottomInputArea | `BottomInputArea.kt` |
| 4015-4110 | 图片处理工具函数 | `ImageUtils.kt` |
| 4110-4200 | buildConversationTranscript, collectStreamContent, stripMarkdownCodeFences | `ChatUtils.kt` |
| 4200-4810 | App Builder 相关 (AppDevToolSpec, skills, prompts, generation) | `AppBuilderEngine.kt` |
| 4810-5190 | MCP 工具相关 (tag builders, parsers, tool instructions) | `McpToolEngine.kt` |
| 5190-5400 | MCP 解析器 (parseMcpToolCallsPayload, extractFirstJsonCandidate 等) | `McpToolEngine.kt` |
| 5400-5760 | 记忆提取 + 标题生成 | `ChatUtils.kt` |

### 新建文件清单

```
app/src/main/java/com/zionchat/app/
├── viewmodel/
│   └── ChatViewModel.kt          # 聊天核心 ViewModel
├── ui/
│   ├── screens/
│   │   └── ChatScreen.kt         # 精简版 (仅 UI 骨架 + ViewModel调用)
│   └── chat/                     # 新增子目录
│       ├── ChatModels.kt         # PendingMessage, PendingImageAttachment, AppDevTagPayload 等
│       ├── MessageItems.kt       # UserMessageItem, AssistantMessageItem
│       ├── MessageTags.kt        # MessageTagRow, McpTagDetailCard, AppDevToolTagCard
│       ├── AppDevComponents.kt   # AppDevTagDetailCard, AppDevWorkspaceScreen, AppDevCodeSurface
│       ├── ChatDialogs.kt        # MessageOptionsDialog, DialogOption
│       ├── ChatNavComponents.kt  # TopNavBar, SidebarContent, EmptyChatState
│       ├── ToolMenuPanel.kt      # ToolMenuPanel, QuickActionButton, ToolListItem
│       └── BottomInputArea.kt    # BottomInputArea
├── util/
│   ├── ImageUtils.kt             # 图片编码/缩放工具
│   └── ChatUtils.kt              # Markdown处理、标题生成、记忆提取等工具
├── engine/
│   ├── AppBuilderEngine.kt       # App生成/修改引擎 + prompts
│   └── McpToolEngine.kt          # MCP工具调用引擎 + 解析器
└── data/
    ├── db/                       # Room 数据库(Phase 2)
    │   ├── AppDatabase.kt
    │   ├── Converters.kt
    │   ├── dao/
    │   │   ├── ConversationDao.kt
    │   │   ├── MessageDao.kt
    │   │   └── MemoryDao.kt
    │   └── entity/
    │       ├── ConversationEntity.kt
    │       ├── MessageEntity.kt
    │       ├── MessageTagEntity.kt
    │       └── MemoryEntity.kt
    └── AppRepository.kt          # 修改为使用 Room
```

---

## 三、分阶段执行计划

### Phase 1: 拆分 ChatScreen.kt (不改变功能)

**Step 1.1**: 创建 `ChatModels.kt` - 提取数据类
**Step 1.2**: 创建 `ChatUtils.kt` - 提取工具函数
**Step 1.3**: 创建 `ImageUtils.kt` - 提取图片工具
**Step 1.4**: 创建 `McpToolEngine.kt` - 提取 MCP 引擎
**Step 1.5**: 创建 `AppBuilderEngine.kt` - 提取 App Builder 引擎
**Step 1.6**: 创建 `BottomInputArea.kt` - 提取底部输入区
**Step 1.7**: 创建 `ToolMenuPanel.kt` - 提取工具菜单面板
**Step 1.8**: 创建 `ChatNavComponents.kt` - 提取导航组件
**Step 1.9**: 创建 `ChatDialogs.kt` - 提取对话框组件
**Step 1.10**: 创建 `MessageTags.kt` - 提取标签组件
**Step 1.11**: 创建 `MessageItems.kt` - 提取消息项组件
**Step 1.12**: 创建 `AppDevComponents.kt` - 提取 App Dev 组件
**Step 1.13**: 精简 `ChatScreen.kt` - 移除已提取代码，添加新导入

### Phase 2: 引入 ChatViewModel

**Step 2.1**: 创建 `ChatViewModel.kt` 基础结构
**Step 2.2**: 迁移状态变量到 ViewModel
**Step 2.3**: 迁移 `sendMessage()` 到 ViewModel
**Step 2.4**: 更新 `ChatScreen.kt` 使用 ViewModel

### Phase 3: 引入 Room 数据库

**Step 3.1**: 添加 Room 依赖到 `build.gradle`
**Step 3.2**: 创建 Room Entity 类
**Step 3.3**: 创建 DAO 接口
**Step 3.4**: 创建 AppDatabase
**Step 3.5**: 更新 AppRepository 使用 Room
**Step 3.6**: 添加 DataStore → Room 数据迁移
**Step 3.7**: 更新 AppContainer

---

## 四、当前进度

- [x] 完整阅读 ChatScreen.kt
- [x] 完整阅读 AppRepository.kt
- [x] 分析项目结构
- [x] 制定详细计划
- [ ] Phase 1 执行中...

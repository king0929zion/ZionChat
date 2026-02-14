# ZionChat APP Developer 模块优化计划

> 创建日期: 2024-02-14
> 状态:待实施

---

## 一、问题背景

当前 AI 生成的 HTML App 在 ZionChat 主 App 内通过 WebView 预览，存在以下痛点：

| 问题 | 描述 |
|------|------|
| 预览区域太小 | WebView 预览受限于 Dialog 高度（约 360dp） |
| 卡片 UI 不好看 | AppDevToolTagCard 设计不够美观 |
| 性能体验差 | WebView 交互流畅度不够 |
| 无法独立运行 | 生成的 App 不能作为独立应用使用 |
| 更新不便捷 | 修改后需要重新保存，无法热更新 |

---

## 二、解决方案

### 2.1 核心思路

**利用 Vercel 网页托管服务实现热更新**

```
┌─────────────────────────────────────────────┐
│              ZionChat 生态系统              │
├─────────────────────────────────────────────┤
│                                             │
│  ZionChat 主App │
│         │                                   │
│         │ 1. AI 生成 HTML                   │
│         ▼                                   │
│  ┌─────────────────┐                        │
│  │    Vercel       │◀─── 2. 上传部署        │
│  │  (网页托管)     │                        │
│  └────────┬────────┘                        │
│           │                                 │
│           │ 3. 返回公开 URL                 │
│           ▼                                 │
│  ┌─────────────────┐                        │
│  │  Runtime APK    │  4. WebView 加载 URL  │
│  │  (独立应用)     │                        │
│  └─────────────────┘                        │
│                                             │
└─────────────────────────────────────────────┘
```

### 2.2 为什么选择 Vercel？

| 优势 | 说明 |
|------|------|
| 部署简单 | 一个 API 调用即可完成部署 |
| 免费额度充足 | 100GB 带宽/月，足够个人使用 |
| 自动 HTTPS | 免费 SSL 证书 |
| 自定义域名 | 可绑定自己的域名 |

### 2.3 热更新原理

```
用户: "修改 Todo App 添加暗色模式"
        │
        ▼
AI 生成新 HTML
        │
        ▼
上传到 Vercel Blob（覆盖旧版本或生成新 URL）
        │
        ▼
用户打开 Runtime APK → WebView 加载 URL → 自动获得更新
```

**无需重新打包 APK，更新即时生效！**

---

## 三、用户授权流程

### 3.1 一次性配置

```
┌─────────────────────────────────────────────┐
│  步骤 1: 用户首次使用时，提示配置 Vercel     │
│  ┌───────────────────────────────────────┐  │
│  │  设置 > 网页托管 > 配置 Vercel         │  │
│  │                                       │  │
│  │  [点击获取 Token] → 跳转 Vercel 网站   │  │
│  │                                       │  │
│  │  粘贴 Token: [________________]       │  │
│  │                                       │  │
│  │  [验证并保存]                          │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  步骤 2: Token 安全存储在 Android Keystore  │
│  步骤 3: 后续部署自动使用 Token              │
└─────────────────────────────────────────────┘
```

### 3.2 用户操作步骤

1. **注册 Vercel 账号**（免费）
   - 访问 https://vercel.com
   - 使用 GitHub/Google/Email 注册

2. **创建 Token**
   - 进入 Account Settings > Tokens
   - 点击 "Create Token"
   - 复制生成的 Token

3. **配置 ZionChat**
   - 打开 ZionChat > 设置 > 网页托管
   - 粘贴 Token
   - 点击验证并保存

---

## 四、开发计划（5 周，并行推进）

### 4.1 轨道 A: UI 优化（Week 1）

**目标**: 立即可见的界面改进

| 任务 | 文件 | 改进内容 |
|------|------|---------|
| 卡片优化 | ChatScreen.kt | 增大 AppDevToolTagCard 尺寸、改进状态指示器 |
| 预览优化 | AppsScreen.kt | 增大 WebView 预览区域（360dp → 80% 高度） |
| 全屏按钮 | AppsScreen.kt | 添加全屏预览快捷按钮 |
| 性能优化 | 多文件 | 启用硬件加速、预加载 WebView |

### 4.2 轨道 B: Vercel 集成（Week 2）

**目标**: 实现 AI 生成后自动部署

**新建文件**:

```kotlin
// WebHostingService.kt - 网页托管服务接口
interface WebHostingService {
    suspend fun deployApp(appId: String, html: String): Result<String>
    suspend fun getAppUrl(appId: String): String?
}

// VercelDeployService.kt - Vercel API 实现
class VercelDeployService : WebHostingService {
    // 使用 Vercel Blob API 上传 HTML
    // API: POST https://api.vercel.com/v2/blob
}
```

**修改文件**:

| 文件 | 修改内容 |
|------|---------|
| Models.kt | SavedApp 添加 `deployUrl: String?` 字段 |
| AppRepository.kt | 添加部署相关方法 |
| SettingsScreen.kt | 添加 Vercel Token 配置界面 |

### 4.3 轨道 C: Runtime APK（Week 3-4）

**目标**: 生成独立运行的 APK

**方案**: 预构建通用 Runtime APK

```
zionchat-runtime/
├── app/src/main/java/com/zionchat/runtime/
│   ├── RuntimeMainActivity.kt      # App 列表入口
│   ├── AppLauncherActivity.kt      # Deep Link 启动
│   └── ui/
│       └── RuntimeWebViewScreen.kt # 全屏 WebView
```

**核心代码**:

```kotlin
// AppLauncherActivity.kt
class AppLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appUrl = intent.data?.getQueryParameter("url")

        setContent {
            AndroidView(
                factory = { WebView(it).apply {
                    settings.javaScriptEnabled = true
                    loadUrl(appUrl)
                }}
            )
        }
    }
}
```

**Deep Link 格式**:
```
zionchat-app://open?url=https://xxx.vercel.app/xxx.html
```

### 4.4 轨道 D: AI 更新流程（Week 5）

**目标**: 打通完整的 AI → 部署 → 更新流程

| 步骤 | 操作 |
|------|------|
| 1 | 用户请求修改 App |
| 2 | AI 生成新 HTML |
| 3 | 自动调用 `deployApp()` 上传到 Vercel |
| 4 | 显示部署状态和 URL |
| 5 | Runtime APK 刷新即可获得更新 |

---

## 五、关键文件清单

### 5.1 需要修改的文件

| 文件路径 | 修改内容 |
|----------|---------|
| `app/src/main/java/com/zionchat/app/data/Models.kt` | SavedApp 添加 `deployUrl` |
| `app/src/main/java/com/zionchat/app/data/AppRepository.kt` | 添加部署方法 |
| `app/src/main/java/com/zionchat/app/ui/screens/ChatScreen.kt` | 优化卡片 UI |
| `app/src/main/java/com/zionchat/app/ui/screens/AppsScreen.kt` | 增大预览区域 |

### 5.2 需要新建的文件

| 文件路径 | 功能 |
|----------|------|
| `app/src/main/java/com/zionchat/app/data/WebHostingService.kt` | 托管服务接口 |
| `app/src/main/java/com/zionchat/app/data/VercelDeployService.kt` | Vercel API 实现 |
| `app/src/main/java/com/zionchat/app/ui/screens/SettingsScreen.kt` | Token 配置界面 |

---

## 六、技术挑战与解决方案

| 挑战 | 解决方案 |
|------|---------|
| Vercel 国内访问 | 用户可绑定自定义域名 + 国内 CDN（阿里云/腾讯云） |
| Token 安全存储 | 使用 Android Keystore 加密 |
| 离线可用 | 本地 HTML 备份 + Service Worker 缓存 |
| APK 动态生成 | 使用预构建通用 Runtime + Deep Link |

---

## 七、国内访问优化（可选）

如果 Vercel 默认速度不理想，用户可以：

1. **绑定自定义域名**
   - 在 Vercel 项目设置中添加自己的域名

2. **配置国内 CDN**
   - 将域名解析到阿里云/腾讯云 CDN
   - 配置 SSL 证书

3. **或使用 Cloudflare 加速**
   - 将域名托管到 Cloudflare
   - 启用 CDN 加速

---

## 八、验证清单

### 8.1 UI 优化验证

- [ ] AppDevToolTagCard 卡片更美观
- [ ] WebView 预览区域更大
- [ ] 全屏预览功能正常

### 8.2 网页托管验证

- [ ] AI 生成 App 后自动部署到 Vercel
- [ ] 部署 URL 可正常访问
- [ ] Token 配置界面正常工作

### 8.3 热更新验证

- [ ] 修改已部署的 App
- [ ] 网页自动更新
- [ ] Runtime APK 打开后显示新版本

### 8.4 Runtime APK 验证

- [ ] 安装 Runtime APK
- [ ] 通过 Deep Link 启动特定 App
- [ ] 创建桌面快捷方式

---

## 九、快速启动建议

如果希望快速看到效果，建议按以下顺序实施：

| 优先级 | 任务 | 预计时间 | 效果 |
|--------|------|----------|------|
| P0 | UI 优化 | 1 周 | 立即可见的改进 |
| P1 | Vercel 集成 | 1 周 | 自动部署功能 |
| P2 | Runtime APK | 2 周 | 独立应用体验 |
| P3 | AI 更新流程 | 1 周 | 完整热更新 |

---

## 十、参考资料

- [Vercel API 文档](https://vercel.com/docs/rest-api)
- [Vercel Blob Storage](https://vercel.com/docs/storage/vercel-blob)
- [Android WebView 优化](https://developer.android.com/reference/android/webkit/WebView)
- [Android Keystore](https://developer.android.com/training/articles/keystore)

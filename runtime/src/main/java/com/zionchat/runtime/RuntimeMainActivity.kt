package com.zionchat.runtime

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt

class RuntimeMainActivity : ComponentActivity() {
    private var lastStatusBarColor: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        applyStatusBarColor(ComposeColor(0xFFF5F5F7))

        setContent {
            var chromeColor by remember { mutableStateOf(ComposeColor(0xFFF5F5F7)) }
            LaunchedEffect(chromeColor) {
                applyStatusBarColor(chromeColor)
            }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = chromeColor) {
                    RuntimeAppScreen(
                        appName = BuildConfig.RUNTIME_APP_NAME,
                        appUrl = BuildConfig.RUNTIME_APP_URL,
                        onChromeColorChanged = { chromeColor = it }
                    )
                }
            }
        }
    }

    private fun applyStatusBarColor(color: ComposeColor) {
        val targetColor = color.toArgb()
        if (lastStatusBarColor == targetColor) return
        lastStatusBarColor = targetColor

        window.statusBarColor = targetColor
        val insetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = color.luminance() > 0.5f
    }
}

@Composable
private fun RuntimeAppScreen(
    appName: String,
    appUrl: String,
    onChromeColorChanged: (ComposeColor) -> Unit
) {
    val targetUrl = appUrl.trim()
    if (targetUrl.isBlank()) {
        MissingRuntimeConfigScreen(appName = appName)
    } else {
        RuntimeWebView(
            url = targetUrl,
            onChromeColorChanged = onChromeColorChanged
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RuntimeWebView(
    url: String,
    onChromeColorChanged: (ComposeColor) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView?,
                            finishedUrl: String?
                        ) {
                            super.onPageFinished(view, finishedUrl)
                            view?.evaluateJavascript(
                                """
                                (function() {
                                  try {
                                    var meta = document.querySelector('meta[name="theme-color"]');
                                    if (meta && meta.content) { return meta.content; }
                                    var body = document.body ? getComputedStyle(document.body).backgroundColor : '';
                                    if (body && body !== 'transparent' && body !== 'rgba(0, 0, 0, 0)') { return body; }
                                    var root = document.documentElement ? getComputedStyle(document.documentElement).backgroundColor : '';
                                    return root || '';
                                  } catch (e) {
                                    return '';
                                  }
                                })();
                                """.trimIndent()
                            ) { jsValue ->
                                parseCssColorFromJs(jsValue)?.let { parsed ->
                                    onChromeColorChanged(ComposeColor(parsed))
                                }
                            }
                        }
                    }
                webChromeClient = WebChromeClient()
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}

@Composable
private fun MissingRuntimeConfigScreen(appName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFFF5F5F7))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = appName.ifBlank { "Runtime App" },
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = ComposeColor(0xFF111214)
            )
            Text(
                text = "Missing runtime URL configuration.",
                fontSize = 15.sp,
                color = ComposeColor(0xFF636366)
            )
            Text(
                text = "Rebuild this APK with -PRUNTIME_APP_URL",
                fontSize = 13.sp,
                color = ComposeColor(0xFF8E8E93)
            )
        }
    }
}

private fun parseCssColorFromJs(jsValue: String?): Int? {
    val raw =
        jsValue
            ?.trim()
            ?.trim('"')
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
            ?.trim()
            .orEmpty()
    if (raw.isBlank()) return null
    if (raw.equals("transparent", ignoreCase = true)) return null
    if (raw.equals("rgba(0, 0, 0, 0)", ignoreCase = true)) return null

    if (raw.startsWith("#")) {
        return runCatching { Color.parseColor(raw) }.getOrNull()
    }

    val rgba =
        Regex("""rgba?\(\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})(?:\s*,\s*([0-9]*\.?[0-9]+)\s*)?\)""")
            .find(raw)
            ?.groupValues

    if (rgba != null) {
        val r = rgba[1].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val g = rgba[2].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val b = rgba[3].toIntOrNull()?.coerceIn(0, 255) ?: return null
        val alpha =
            rgba.getOrNull(4)?.takeIf { it.isNotBlank() }?.toFloatOrNull()?.coerceIn(0f, 1f)
                ?: 1f
        return Color.argb((alpha * 255f).roundToInt(), r, g, b)
    }

    return runCatching { Color.parseColor(raw) }.getOrNull()
}

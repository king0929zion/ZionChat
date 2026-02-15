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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class RuntimeMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = ComposeColor(0xFFF5F5F7)) {
                    RuntimeAppScreen(
                        appName = BuildConfig.RUNTIME_APP_NAME,
                        appUrl = BuildConfig.RUNTIME_APP_URL
                    )
                }
            }
        }
    }
}

@Composable
private fun RuntimeAppScreen(
    appName: String,
    appUrl: String
) {
    val targetUrl = appUrl.trim()
    if (targetUrl.isBlank()) {
        MissingRuntimeConfigScreen(appName = appName)
    } else {
        RuntimeWebView(url = targetUrl)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RuntimeWebView(url: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
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


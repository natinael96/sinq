package com.agpeya.app.ui.catena

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.openUrl
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize

/** The one host this screen will render. Anything else leaves for a real browser. */
private const val CATENA_HOST = "catenabible.com"

/**
 * The Fathers on a verse, at catenabible.com, showing only the early ones.
 *
 * Catena sorts its commentators by tradition and will serve a single tradition
 * if asked. It is asked with a cookie, `commentaryTag`, which its own Settings
 * panel writes and its server reads: with `EF` set, Matthew 5:4 comes back with
 * eleven commentaries instead of nineteen, and Haydock, the Glossa and
 * Theophylact are not among them. The site's own words for that group are "all
 * fathers venerated in all churches in communion with the Church of
 * Alexandria", which is the Church this app is for.
 *
 * That cookie is why this is a WebView and not the Custom Tab it used to be. A
 * Custom Tab is Chrome, with Chrome's cookies, and an app cannot write to them.
 * A WebView has a cookie jar of its own, so the preference is set here, applies
 * only here, and never touches what the reader has chosen in their own browser.
 *
 * The page is still plainly theirs: the address is in the bar, and the action in
 * the corner hands the same URL to a real browser.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CatenaScreen(url: String, reference: String, onBack: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Before anything loads. Session-scoped would be lost between visits, so it
    // is given a year and rewritten on each entry, which also repairs it if the
    // site's own Settings panel has since overwritten it.
    remember(url) { setEarlyFathersCookie(); true }

    BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.commentaryAction,
                subtitle = reference.takeIf { it.isNotBlank() } ?: CATENA_HOST,
                accentLine = s.catenaEarlyFathers,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { openUrl(context, url) }) {
                        Icon(
                            Icons.Outlined.OpenInNew,
                            contentDescription = s.catenaOpenInBrowser,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    buildWebView(ctx) { progress = it }.also { webView = it }
                },
                update = { view ->
                    if (view.url == null) view.loadUrl(url)
                },
            )
        }
        if (progress == 0) {
            Column(
                Modifier.fillMaxSize().padding(inner),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }
        }
    }
}

/**
 * Ask Catena for the early fathers alone.
 *
 * `commentaryTag` is read by their server, so it has to be in the jar before the
 * first request rather than set from inside the page.
 */
private fun setEarlyFathersCookie() {
    runCatching {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setCookie("https://$CATENA_HOST", "commentaryTag=EF; path=/; max-age=31536000")
            flush()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun buildWebView(context: Context, onProgress: (Int) -> Unit): WebView =
    WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setBackgroundColor(AndroidColor.TRANSPARENT)
        // Catena renders its commentaries in the browser, so the page is blank
        // without this. Nothing else is granted: no file access, no storage
        // beyond the cookie the page needs, and no bridge into the app.
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgress(newProgress)
            }
        }
        webViewClient = object : WebViewClient() {
            // This screen is Catena's, and only Catena's. A link anywhere else —
            // the OrthodoxWiki pages behind each Father's name, their Facebook —
            // is somebody else's site and belongs in a real browser.
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val host = request.url.host.orEmpty()
                if (host == CATENA_HOST || host.endsWith(".$CATENA_HOST")) return false
                openUrl(view.context, request.url.toString())
                return true
            }
        }
    }

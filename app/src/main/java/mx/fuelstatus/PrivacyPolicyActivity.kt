package mx.fuelstatus

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.fuelstatus.ui.FuelIcon
import mx.fuelstatus.ui.FuelTheme
import mx.fuelstatus.ui.themeIsDark

private const val POLICY_URL = "https://appassets.androidplatform.net/privacidad.html"

class PrivacyPolicyActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appearance = (application as FuelApplication).graph.appearance
        setContent {
            val preferences by appearance.state.collectAsStateWithLifecycle()
            FuelTheme(preferences) {
                val dark = themeIsDark(preferences)
                val html = remember(dark) {
                    assets.open("privacidad.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
                        // Honor the app's selected theme while retaining the original document and styles.
                        .replace("@media (prefers-color-scheme: dark)", if (dark) "@media all" else "@media not all")
                }
                Scaffold(topBar = {
                    TopAppBar(title = { Text("Política de privacidad") }, navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            FuelIcon(R.drawable.ic_arrow_back, "Volver a Ajustes")
                        }
                    })
                }) { padding ->
                    key(dark) {
                        AndroidView(modifier = Modifier.fillMaxSize().padding(padding), factory = { context ->
                            WebView(context).apply {
                                settings.apply {
                                    javaScriptEnabled = false
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    blockNetworkLoads = true
                                    textZoom = (resources.configuration.fontScale * 100).toInt()
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        if (request.url.toString().substringBefore('#') == POLICY_URL) return false
                                        openExternalLink(request.url)
                                        return true
                                    }
                                }
                                loadDataWithBaseURL(POLICY_URL, html, "text/html", "UTF-8", null)
                            }
                        }, onRelease = { it.destroy() })
                    }
                }
            }
        }
    }

    private fun openExternalLink(uri: Uri) {
        val action = when (uri.scheme) {
            "https" -> Intent.ACTION_VIEW
            "mailto" -> Intent.ACTION_SENDTO
            else -> return
        }
        try {
            startActivity(Intent(action, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No hay una aplicación disponible para abrir este enlace.", Toast.LENGTH_LONG).show()
        }
    }
}

package com.asaas.pos

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.asaas.pos.databinding.ActivityMainBinding
import com.asaas.pos.utils.SessionManager
import com.asaas.pos.utils.NetworkUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private var webView: WebView? = null

    companion object {
        private const val TAG = "AsaasPOS_Main"
        const val BASE_URL = "https://super.asaas-system.com"
        const val POS_URL = "$BASE_URL/pos_sales.php"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupWebView()
        setupSwipeRefresh()
        loadPosUrl()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = binding.webView
        webView?.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(true)
                builtInZoomControls = false
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                userAgentString = "$userAgentString ASAAS-POS-Android/1.0"
            }

            webViewClient = AsaasWebViewClient()
            webChromeClient = AsaasWebChromeClient()

            // Enable USB printing bridge
            addJavascriptInterface(PrintBridge(this@MainActivity), "AndroidPrint")
            
            // Session interface
            addJavascriptInterface(SessionBridge(sessionManager), "AndroidSession")
        }

        // Download listener for reports, receipts
        webView?.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
                request.setDescription("Downloading file...")
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(applicationContext, "جاري التحميل...", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Download error: ${e.message}")
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            webView?.reload()
            binding.swipeRefresh.isRefreshing = false
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.asaas_primary, R.color.asaas_secondary)
    }

    private fun loadPosUrl() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoInternetDialog()
            return
        }
        
        showLoading(true)
        
        // Add auth token from session
        val token = sessionManager.getAuthToken()
        if (token != null) {
            webView?.loadUrl(POS_URL)
        } else {
            // Token expired, go back to login
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.webView.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("لا يوجد اتصال بالإنترنت")
            .setMessage("يرجى التحقق من اتصالك بالإنترنت والمحاولة مرة أخرى")
            .setPositiveButton("إعادة المحاولة") { _, _ -> loadPosUrl() }
            .setNegativeButton("إغلاق") { _, _ -> finish() }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView?.canGoBack() == true) {
            webView?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        CookieManager.getInstance().flush()
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }

    inner class AsaasWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            showLoading(true)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            showLoading(false)
            CookieManager.getInstance().flush()
            
            // Check if redirected to login page (session expired)
            if (url?.contains("login.php") == true) {
                sessionManager.clearSession()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }

        @Deprecated("Deprecated in API 23")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return handleUrl(url)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return handleUrl(request?.url?.toString())
        }

        private fun handleUrl(url: String?): Boolean {
            if (url == null) return false
            return when {
                url.startsWith("tel:") -> {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))
                    true
                }
                url.startsWith("mailto:") -> {
                    startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
                    true
                }
                url.contains(BASE_URL) -> false
                else -> false
            }
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.proceed() // Accept SSL cert for internal server
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                showLoading(false)
                if (!NetworkUtils.isNetworkAvailable(this@MainActivity)) {
                    showNoInternetDialog()
                }
            }
        }
    }

    inner class AsaasWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress == 100) {
                showLoading(false)
            }
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            Log.d(TAG, "JS Console: ${consoleMessage?.message()}")
            return true
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            AlertDialog.Builder(this@MainActivity)
                .setMessage(message)
                .setPositiveButton("موافق") { _, _ -> result?.confirm() }
                .show()
            return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            AlertDialog.Builder(this@MainActivity)
                .setMessage(message)
                .setPositiveButton("نعم") { _, _ -> result?.confirm() }
                .setNegativeButton("لا") { _, _ -> result?.cancel() }
                .show()
            return true
        }
    }
}

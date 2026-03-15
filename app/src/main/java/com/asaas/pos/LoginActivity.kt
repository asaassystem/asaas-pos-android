package com.asaas.pos

import android.annotation.SuppressLint
import android.content.Intent
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.asaas.pos.databinding.ActivityLoginBinding
import com.asaas.pos.utils.SessionManager

/**
 * LoginActivity - WebView كامل يفتح super_admin.php
 * المستخدم يدخل بيانات المنشأة من لوحة التحكم مباشرة
 * عند الانتقال لـ pos_sales.php يتم الانتقال لـ MainActivity
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    companion object {
        const val BASE_URL = "https://super.asaas-system.com"
        const val LOGIN_URL = "$BASE_URL/login.php"
        const val POS_URL = "$BASE_URL/pos_sales.php"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupWebView()
        loadLoginPage()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webViewLogin.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                setSupportZoom(true)
                builtInZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "$userAgentString ASAAS-POS/1.0"
            }

            webViewClient = object : WebViewClient() {

                override fun onReceivedSslError(
                    view: WebView?, handler: SslErrorHandler?, error: SslError?
                ) {
                    handler?.proceed() // قبول SSL الداخلي
                }

                override fun onPageStarted(
                    view: WebView?, url: String?, favicon: android.graphics.Bitmap?
                ) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    CookieManager.getInstance().flush()

                    val currentUrl = url ?: return

                    // ✅ إذا وصل لـ POS انتقل مباشرة للـ MainActivity
                    if (currentUrl.contains("pos_sales.php")) {
                        // استخرج tenant_id من URL إن وُجد
                        val tenantId = extractParam(currentUrl, "tenant_id")
                            ?: extractParam(currentUrl, "tenant")
                            ?: ""

                        sessionManager.saveSession(
                            token = "web_session_active",
                            userName = "pos_user",
                            userId = "",
                            userRole = "employee",
                            tenant = tenantId,
                            tenantId = tenantId,
                            isAdmin = false
                        )

                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false
                    // السماح بكل روابط النظام
                    return if (url.contains(BASE_URL)) {
                        false // تحميل داخل WebView
                    } else {
                        false
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress == 100) {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webViewLogin, true)
    }

    private fun loadLoginPage() {
        binding.webViewLogin.loadUrl(LOGIN_URL)
    }

    private fun extractParam(url: String, param: String): String? {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.getQueryParameter(param)
        } catch (e: Exception) {
            null
        }
    }

    override fun onBackPressed() {
        if (binding.webViewLogin.canGoBack()) {
            binding.webViewLogin.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webViewLogin.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.webViewLogin.onPause()
    }

    override fun onDestroy() {
        binding.webViewLogin.destroy()
        super.onDestroy()
    }
}

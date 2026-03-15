package com.asaas.pos

import android.annotation.SuppressLint
import android.content.Intent
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.asaas.pos.databinding.ActivityLoginBinding
import com.asaas.pos.utils.SessionManager

/**
 * LoginActivity - WebView-based login for ASAAS POS
 * Loads the web login page, intercepts session cookies,
 * and persists credentials for auto-login.
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

        // If saved credentials exist, try to auto-login via form fill
        val savedUser = sessionManager.getSavedUsername()
        val savedPass = sessionManager.getSavedPassword()
        val savedTenant = sessionManager.getTenantId() ?: ""

        if (!savedUser.isNullOrEmpty() && !savedPass.isNullOrEmpty()) {
            binding.etUsername.setText(savedUser)
            binding.etPassword.setText(savedPass)
            if (savedTenant.isNotEmpty()) {
                binding.etTenant.setText(savedTenant)
            }
        }

        setupWebLoginButton()
    }

    private fun setupWebLoginButton() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val tenant = binding.etTenant.text.toString().trim()

            if (username.isEmpty()) {
                binding.etUsername.error = "يرجى إدخال اسم المستخدم"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etPassword.error = "يرجى إدخال كلمة المرور"
                return@setOnClickListener
            }

            // Save credentials for future auto-fill
            sessionManager.saveCredentials(username, password)
            if (tenant.isNotEmpty()) {
                sessionManager.saveTenantId(tenant)
            }

            // Show webview for actual login
            openWebLogin(username, password, tenant)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun openWebLogin(username: String, password: String, tenant: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.loginForm.visibility = View.GONE
        binding.webViewLogin.visibility = View.VISIBLE

        val webView = binding.webViewLogin
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(false)
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Accept all SSL certs for internal server
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE

                val currentUrl = url ?: ""

                // Auto-fill login form on login page
                if (currentUrl.contains("login.php") || currentUrl == BASE_URL || currentUrl == "$BASE_URL/") {
                    val tenantJs = if (tenant.isNotEmpty())
                        "var tf = document.querySelector('[name=tenant_id],[id=tenant_id],[name=tenant],[id=tenant]'); if(tf) tf.value='$tenant';"
                    else ""

                    val js = """
                        (function() {
                            $tenantJs
                            var u = document.querySelector('[name=username],[id=username],[type=text]:not([name=tenant_id])');
                            var p = document.querySelector('[name=password],[id=password],[type=password]');
                            if(u) u.value = '$username';
                            if(p) p.value = '$password';
                            // Auto submit
                            var btn = document.querySelector('[type=submit],button[name=login],#btnLogin,.btn-login');
                            if(btn) btn.click();
                        })();
                    """.trimIndent()
                    view?.evaluateJavascript(js, null)
                }

                // Detect successful login - redirected to POS
                if (currentUrl.contains("pos_sales.php") ||
                    currentUrl.contains("dashboard") ||
                    currentUrl.contains("index.php") && !currentUrl.contains("login")) {

                    // Save session and go to MainActivity
                    sessionManager.saveSession(
                        token = "web_session",
                        userName = username,
                        userId = "",
                        userRole = "employee",
                        tenant = tenant,
                        tenantId = tenant,
                        isAdmin = false
                    )

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@LoginActivity, "لا يمكن الاتصال بالخادم", Toast.LENGTH_LONG).show()
                    // Go back to form
                    binding.loginForm.visibility = View.VISIBLE
                    binding.webViewLogin.visibility = View.GONE
                }
            }
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.loadUrl(LOGIN_URL)
    }

    override fun onBackPressed() {
        if (binding.webViewLogin.visibility == View.VISIBLE) {
            binding.webViewLogin.visibility = View.GONE
            binding.loginForm.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}

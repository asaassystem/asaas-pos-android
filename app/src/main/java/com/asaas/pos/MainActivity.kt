package com.asaas.pos

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.asaas.pos.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var fabNewSale: FloatingActionButton
    private lateinit var sessionManager: SessionManager

    companion object {
        const val BASE_URL = "https://super.asaas-system.com"
        const val TAG = "AsaasPOS"
    }

    // URLs for each tab
    private val salesUrl get() = "$BASE_URL/pos_sales.php?tenant_id=${sessionManager.getTenantId()}"
    private val ordersUrl get() = "$BASE_URL/pos_orders.php?tenant_id=${sessionManager.getTenantId()}"
    private val reportsUrl get() = "$BASE_URL/pos_reports.php?tenant_id=${sessionManager.getTenantId()}"
    private val settingsUrl get() = "$BASE_URL/pos_settings.php?tenant_id=${sessionManager.getTenantId()}"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        // Check if logged in
        if (!sessionManager.isLoggedIn()) {
            goToLogin()
            return
        }

        // Setup Views
        toolbar = findViewById(R.id.toolbar)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        bottomNav = findViewById(R.id.bottomNav)
        fabNewSale = findViewById(R.id.fabNewSale)

        // Setup Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "ASAAS POS"
        supportActionBar?.subtitle = sessionManager.getTenantName() ?: sessionManager.getTenantId()

        // Setup WebView
        setupWebView()

        // Setup Bottom Navigation
        setupBottomNav()

        // Setup Swipe Refresh
        swipeRefresh.setColorSchemeResources(R.color.colorAccent)
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }

        // Setup FAB
        fabNewSale.setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_sales
            loadUrl(salesUrl)
            Toast.makeText(this, "فتح صفحة البيع", Toast.LENGTH_SHORT).show()
        }

        // Load default page (Sales)
        loadUrl(salesUrl)
        bottomNav.selectedItemId = R.id.nav_sales
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        settings.userAgentString = settings.userAgentString + " AsaasPOS/1.0 Android"

        // Inject session cookies via JS interface
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                // Inject session data into page
                injectSessionData()

                // Inject CSS to hide web navigation (header/sidebar) for native app feel
                injectNativeStyle()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                // Handle login redirect
                if (url.contains("login") || url.contains("logout")) {
                    sessionManager.logout()
                    goToLogin()
                    return true
                }

                // Handle external links
                if (!url.startsWith(BASE_URL) && (url.startsWith("http") || url.startsWith("https"))) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        // ignore
                    }
                    return true
                }

                return false
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_sales -> {
                    loadUrl(salesUrl)
                    true
                }
                R.id.nav_orders -> {
                    loadUrl(ordersUrl)
                    true
                }
                R.id.nav_reports -> {
                    loadUrl(reportsUrl)
                    true
                }
                R.id.nav_settings -> {
                    loadUrl(settingsUrl)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    private fun injectSessionData() {
        val tenantId = sessionManager.getTenantId() ?: ""
        val username = sessionManager.getUsername() ?: ""
        val token = sessionManager.getToken() ?: ""

        val js = """
            (function() {
                try {
                    if (window.AsaasSession === undefined) {
                        window.AsaasSession = {
                            tenant_id: '$tenantId',
                            username: '$username',
                            token: '$token',
                            is_android: true
                        };
                    }
                    // Auto-fill login form if present
                    var tenantInput = document.querySelector('[name="tenant_id"]');
                    var usernameInput = document.querySelector('[name="username"]');
                    if (tenantInput) tenantInput.value = '$tenantId';
                    if (usernameInput) usernameInput.value = '$username';
                } catch(e) {}
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    private fun injectNativeStyle() {
        // Hide web page header/nav since we use native toolbar + bottom nav
        val css = """
            (function() {
                try {
                    var style = document.createElement('style');
                    style.innerHTML = '
                        .navbar, .sidebar, .web-header, #header, .top-nav,
                        .nav-tabs-container, .breadcrumb-container { display: none !important; }
                        body { padding-top: 0 !important; margin-top: 0 !important; }
                        .content-wrapper, .main-content, #content { margin-left: 0 !important; padding-left: 8px !important; }
                    ';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(css, null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            R.id.action_logout -> {
                confirmLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("تسجيل الخروج")
            .setMessage("هل تريد تسجيل الخروج؟")
            .setPositiveButton("نعم") { _, _ ->
                sessionManager.logout()
                goToLogin()
            }
            .setNegativeButton("لا", null)
            .show()
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            AlertDialog.Builder(this)
                .setTitle("خروج")
                .setMessage("هل تريد الخروج من التطبيق؟")
                .setPositiveButton("نعم") { _, _ -> super.onBackPressed() }
                .setNegativeButton("لا", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    // JavaScript Bridge for communication between WebView and Android
    inner class AndroidBridge {
        @JavascriptInterface
        fun getTenantId(): String = sessionManager.getTenantId() ?: ""

        @JavascriptInterface
        fun getUsername(): String = sessionManager.getUsername() ?: ""

        @JavascriptInterface
        fun getToken(): String = sessionManager.getToken() ?: ""

        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun logout() {
            runOnUiThread { confirmLogout() }
        }

        @JavascriptInterface
        fun navigateTo(section: String) {
            runOnUiThread {
                when (section) {
                    "sales" -> {
                        bottomNav.selectedItemId = R.id.nav_sales
                        loadUrl(salesUrl)
                    }
                    "orders" -> {
                        bottomNav.selectedItemId = R.id.nav_orders
                        loadUrl(ordersUrl)
                    }
                    "reports" -> {
                        bottomNav.selectedItemId = R.id.nav_reports
                        loadUrl(reportsUrl)
                    }
                    "settings" -> {
                        bottomNav.selectedItemId = R.id.nav_settings
                        loadUrl(settingsUrl)
                    }
                }
            }
        }
    }
}

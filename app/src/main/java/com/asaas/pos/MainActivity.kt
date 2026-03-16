package com.asaas.pos

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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

    private val salesUrl get() = "${BASE_URL}/pos_sales.php?tenant_id=${sessionManager.getTenantId()}&_token=${sessionManager.getToken()}"
    private val ordersUrl get() = "${BASE_URL}/pos_orders.php?tenant_id=${sessionManager.getTenantId()}&_token=${sessionManager.getToken()}"
    private val reportsUrl get() = "${BASE_URL}/pos_reports.php?tenant_id=${sessionManager.getTenantId()}&_token=${sessionManager.getToken()}"
    private val settingsUrl get() = "${BASE_URL}/pos_settings.php?tenant_id=${sessionManager.getTenantId()}&_token=${sessionManager.getToken()}"

    private var currentSection = "sales"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            goToLogin()
            return
        }

        toolbar = findViewById(R.id.toolbar)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        bottomNav = findViewById(R.id.bottomNav)
        fabNewSale = findViewById(R.id.fabNewSale)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "ASAAS POS"
        supportActionBar?.subtitle = sessionManager.getTenantName() ?: sessionManager.getTenantId()

        setupWebView()
        setupCookies()
        setupBottomNav()

        swipeRefresh.setColorSchemeResources(R.color.colorAccent)
        swipeRefresh.setOnRefreshListener { webView.reload() }

        fabNewSale.setOnClickListener {
            currentSection = "sales"
            bottomNav.selectedItemId = R.id.nav_sales
            loadUrl(salesUrl)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("خروج")
                        .setMessage("هل تريد الخروج من التطبيق؟")
                        .setPositiveButton("نعم") { _, _ ->
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                        .setNegativeButton("لا", null)
                        .show()
                }
            }
        })

        loadUrl(salesUrl)
        bottomNav.selectedItemId = R.id.nav_sales
    }

    private fun setupCookies() {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(webView, true)
        val tid = sessionManager.getTenantId() ?: ""
        val usr = sessionManager.getUsername() ?: ""
        val tok = sessionManager.getToken() ?: ""
        val uid = sessionManager.getUserId() ?: ""
        cm.setCookie(BASE_URL, "pos_tenant_id=${tid}; path=/")
        cm.setCookie(BASE_URL, "pos_username=${usr}; path=/")
        cm.setCookie(BASE_URL, "pos_token=${tok}; path=/")
        cm.setCookie(BASE_URL, "user_id=${uid}; path=/")
        cm.flush()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.setSupportZoom(false)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.mediaPlaybackRequiresUserGesture = false
        s.allowFileAccess = true
        s.userAgentString = "AsaasPOS/1.0 Android/${Build.VERSION.RELEASE} Mobile"

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                swipeRefresh.isRefreshing = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                injectSessionData()
                injectNativeStyle()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith(BASE_URL)) {
                    if (url.contains("action=logout") || url.endsWith("logout.php")) {
                        sessionManager.logout()
                        goToLogin()
                        return true
                    }
                    return false
                }
                if (url.startsWith("http://") || url.startsWith("https://") ||
                    url.startsWith("tel:") || url.startsWith("mailto:")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
                    return true
                }
                return false
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                if (errorCode == -2 || errorCode == -6) {
                    view?.loadData(offlineHtml(), "text/html; charset=utf-8", "UTF-8")
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                if (newProgress == 100) swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun offlineHtml() = """<!DOCTYPE html><html dir="rtl"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>body{font-family:Arial;text-align:center;padding:40px;background:#1a1a2e;color:#fff}
h2{color:#e94560}p{color:#aaa}button{background:#e94560;color:#fff;border:none;padding:12px 24px;
border-radius:8px;font-size:16px;cursor:pointer;margin-top:20px}</style></head>
<body><div style="font-size:64px">&#x1F4E1;</div><h2>لا يوجد اتصال</h2>
<p>تحقق من اتصالك وحاول مرة أخرى</p>
<button onclick="location.reload()">إعادة المحاولة</button></body></html>"""

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_sales -> { if (currentSection != "sales") { currentSection = "sales"; loadUrl(salesUrl) }; true }
                R.id.nav_orders -> { if (currentSection != "orders") { currentSection = "orders"; loadUrl(ordersUrl) }; true }
                R.id.nav_reports -> { if (currentSection != "reports") { currentSection = "reports"; loadUrl(reportsUrl) }; true }
                R.id.nav_settings -> { if (currentSection != "settings") { currentSection = "settings"; loadUrl(settingsUrl) }; true }
                else -> false
            }
        }
    }

    private fun loadUrl(url: String) { webView.loadUrl(url) }

    private fun injectSessionData() {
        val tid = sessionManager.getTenantId() ?: ""
        val usr = sessionManager.getUsername() ?: ""
        val tok = sessionManager.getToken() ?: ""
        val uid = sessionManager.getUserId() ?: ""
        webView.evaluateJavascript("""(function(){try{window.AsaasSession={tenant_id:'${tid}',username:'${usr}',token:'${tok}',user_id:'${uid}',is_android:true,platform:'android'};}catch(e){}}());""", null)
    }

    private fun injectNativeStyle() {
        webView.evaluateJavascript("""(function(){try{if(document.getElementById('ans'))return;var s=document.createElement('style');s.id='ans';s.innerHTML='nav.navbar,.navbar,.sidebar,.left-sidebar,#sidebar,.web-header,#header,.top-nav,.topbar,.nav-tabs-container,.breadcrumb-container,footer,.footer{display:none!important}body{padding-top:0!important;margin-top:0!important}.content-wrapper,.main-content,#content,.page-wrapper{margin-left:0!important;padding:0 4px!important}';document.head.appendChild(s);}catch(e){}}());""", null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> { webView.reload(); true }
            R.id.action_logout -> { confirmLogout(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("تسجيل الخروج")
            .setMessage("هل تريد تسجيل الخروج؟")
            .setPositiveButton("نعم") { _, _ ->
                sessionManager.logout()
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
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

    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onPause() { super.onPause(); webView.onPause() }
    override fun onDestroy() { webView.destroy(); super.onDestroy() }

    inner class AndroidBridge {
        @JavascriptInterface fun getTenantId(): String = sessionManager.getTenantId() ?: ""
        @JavascriptInterface fun getUsername(): String = sessionManager.getUsername() ?: ""
        @JavascriptInterface fun getToken(): String = sessionManager.getToken() ?: ""
        @JavascriptInterface fun getUserId(): String = sessionManager.getUserId() ?: ""
        @JavascriptInterface fun getTenantName(): String = sessionManager.getTenantName() ?: ""
        @JavascriptInterface fun isAndroid(): Boolean = true
        @JavascriptInterface fun getAppVersion(): String = "1.0.0"

        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
        }

        @JavascriptInterface
        fun showAlert(title: String, message: String) {
            runOnUiThread {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(title).setMessage(message)
                    .setPositiveButton("موافق", null).show()
            }
        }

        @JavascriptInterface
        fun logout() { runOnUiThread { confirmLogout() } }

        @JavascriptInterface
        fun navigateTo(section: String) {
            runOnUiThread {
                when (section) {
                    "sales" -> { currentSection = "sales"; bottomNav.selectedItemId = R.id.nav_sales; loadUrl(salesUrl) }
                    "orders" -> { currentSection = "orders"; bottomNav.selectedItemId = R.id.nav_orders; loadUrl(ordersUrl) }
                    "reports" -> { currentSection = "reports"; bottomNav.selectedItemId = R.id.nav_reports; loadUrl(reportsUrl) }
                    "settings" -> { currentSection = "settings"; bottomNav.selectedItemId = R.id.nav_settings; loadUrl(settingsUrl) }
                }
            }
        }

        @JavascriptInterface
        fun setToolbarTitle(title: String) { runOnUiThread { supportActionBar?.subtitle = title } }
    }
}

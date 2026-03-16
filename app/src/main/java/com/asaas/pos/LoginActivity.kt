package com.asaas.pos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.asaas.pos.utils.SessionManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {

    private lateinit var etTenant: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "LoginActivity"
        private const val AUTH_URL = "https://super.asaas-system.com/pos_app_auth.php"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.asaas.pos.utils.SessionManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {

    private lateinit var etTenant: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "LoginActivity"
        private const val API_URL = "https://super.asaas-system.com/_pos_api.php"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            goToMain()
            return
        }

        etTenant = findViewById(R.id.etTenant)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)

        loadSavedCredentials()

        btnLogin.setOnClickListener {
            val tenant = etTenant.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            tvError.visibility = View.GONE

            if (tenant.isEmpty()) { showError("Enter tenant number"); return@setOnClickListener }
            if (username.isEmpty()) { showError("Enter username"); return@setOnClickListener }
            if (password.isEmpty()) { showError("Enter password"); return@setOnClickListener }

            doLogin(tenant, username, password)
        }
    }

    private fun loadSavedCredentials() {
        val savedTenant = sessionManager.getSavedTenantId()
        val savedUsername = sessionManager.getSavedUsername()
        if (!savedTenant.isNullOrEmpty()) etTenant.setText(savedTenant)
        if (!savedUsername.isNullOrEmpty()) etUsername.setText(savedUsername)
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
    }

    private fun doLogin(tenant: String, username: String, password: String) {
        setLoading(true)
        tvError.visibility = View.GONE

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val url = URL(API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("X-Requested-With", "AsaasPOS-Android")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val postData = "action=login&tenant_id=${enc(tenant)}&username=${enc(username)}&password=${enc(password)}&source=android_app"
                conn.outputStream.use { it.write(postData.toByteArray()) }

                val responseCode = conn.responseCode
                val response = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: """{"ok":false,"error":"HTTP $responseCode"}"""
                }
                conn.disconnect()

                Log.d(TAG, "Login response: $response")

                runOnUiThread {

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.asaas.pos.utils.SessionManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {

    private lateinit var etTenant: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "LoginActivity"
        private const val AUTH_URL = "https://super.asaas-system.com/pos_app_auth.php"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) { goToMain(); return }
        etTenant    = findViewById(R.id.etTenant)
        etUsername  = findViewById(R.id.etUsername)
        etPassword  = findViewById(R.id.etPassword)
        btnLogin    = findViewById(R.id.btnLogin)
        tvError     = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)
        loadSavedCredentials()
        btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun loadSavedCredentials() {
        val savedTenant   = sessionManager.getSavedTenantId()
        val savedUsername = sessionManager.getSavedUsername()
        if (!savedTenant.isNullOrEmpty())   etTenant.setText(savedTenant)
        if (!savedUsername.isNullOrEmpty()) etUsername.setText(savedUsername)
    }

    private fun attemptLogin() {
        val tenantStr = etTenant.text.toString().trim()
        val username  = etUsername.text.toString().trim()
        val password  = etPassword.text.toString().trim()
        tvError.visibility = View.GONE
        if (tenantStr.isEmpty()) { showError("ادخل رقم المنشأة"); return }
        if (username.isEmpty())  { showError("ادخل اسم المستخدم"); return }
        if (password.isEmpty())  { showError("ادخل كلمة المرور"); return }
        val tenantId = tenantStr.toIntOrNull()
        if (tenantId == null || tenantId <= 0) { showError("رقم المنشأة غير صحيح"); return }
        setLoading(true)
        Executors.newSingleThreadExecutor().execute {
            val result = performLogin(tenantId, username, password)
            runOnUiThread { setLoading(false); handleLoginResult(result, tenantId, username, password) }
        }
    }

    private fun performLogin(tenantId: Int, username: String, password: String): String {
        return try {
            val url  = URL(AUTH_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout    = 15000
            conn.doOutput = true
            val enc  = java.net.URLEncoder.encode(username, "UTF-8")
            val encP = java.net.URLEncoder.encode(password, "UTF-8")
            val body = "action=login&tenant_id=${tenantId}&username=${enc}&password=${encP}&source=android_app"
            conn.outputStream.write(body.toByteArray(Charsets.UTF_8))
            conn.outputStream.flush()
            val code   = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val resp   = stream?.bufferedReader()?.readText() ?: ""
            conn.disconnect()
            Log.d(TAG, "Login response [${code}]: ${resp}")
            resp
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            "{\"ok\":false,\"error\":\"network_error\"}"
        }
    }

    private fun handleLoginResult(response: String, tenantId: Int, username: String, password: String) {
        try {
            val json = JSONObject(response)
            if (json.optBoolean("ok")) {
                val data       = json.getJSONObject("data")
                val token      = data.optString("token", "")
                val name       = data.optString("name", username)
                val tenantName = data.optString("tenant_name", "")
                val role       = data.optString("role", "employee")
                val userId     = data.optInt("user_id", 0).toString()
                val isAdmin    = (role == "admin" || role == "manager")
                sessionManager.saveSession(
                    token      = token,
                    userName   = name,
                    userId     = userId,
                    userRole   = role,
                    tenant     = username,
                    tenantId   = tenantId.toString(),
                    isAdmin    = isAdmin,
                    tenantName = tenantName
                )
                sessionManager.saveCredentials(username, password)
                sessionManager.saveTenantId(tenantId.toString())
                goToMain()
            } else {
                val error = json.optString("error", "unknown")
                showError(getArabicError(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${response}", e)
            showError("خطأ في الاتصال بالسيرفر")
        }
    }

    private fun getArabicError(code: String) = when (code) {
        "missing_fields"   -> "يرجى ملء جميع الحقول"
        "tenant_not_found" -> "رقم المنشأة غير موجود أو غير نشط"
        "user_not_found"   -> "اسم المستخدم غير موجود"
        "user_inactive"    -> "الحساب موقوف، تواصل مع المدير"
        "wrong_password"   -> "كلمة المرور غير صحيحة"
        "network_error"    -> "تعذر الاتصال بالإنترنت"
        else               -> "خطأ: ${code}"
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        btnLogin.text = if (loading) "جاري التحقق..." else "دخول"
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

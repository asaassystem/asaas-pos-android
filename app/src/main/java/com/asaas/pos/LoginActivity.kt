package com.asaas.pos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
    private lateinit var cbRemember: CheckBox
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

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            goToMain()
            return
        }

        // Find views
        etTenant = findViewById(R.id.etTenant)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        cbRemember = findViewById(R.id.cbRemember)
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)

        // Load saved credentials
        if (sessionManager.isRememberMe()) {
            val savedTenant = sessionManager.getTenantId() ?: ""
            val savedUsername = sessionManager.getSavedUsername() ?: ""
            etTenant.setText(savedTenant)
            etUsername.setText(savedUsername)
            cbRemember.isChecked = true
        }

        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val tenantId = etTenant.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate inputs
        if (tenantId.isEmpty()) {
            etTenant.error = "أدخل رقم المنشأة"
            return
        }
        if (username.isEmpty()) {
            etUsername.error = "أدخل اسم المستخدم"
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "أدخل كلمة المرور"
            return
        }

        // Show loading
        showLoading(true)
        tvError.visibility = View.GONE

        // Execute login in background thread
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val result = callLoginApi(tenantId, username, password)
                runOnUiThread {
                    showLoading(false)
                    handleLoginResult(result, tenantId, username, password)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}")
                runOnUiThread {
                    showLoading(false)
                    showError("تعذر الاتصال بالخادم. تحقق من الإنترنت.")
                }
            }
        }
    }

    private fun callLoginApi(tenantId: String, username: String, password: String): String {
        val url = URL(API_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val params = "action=login&tenant_id=${tenantId}&username=${username}&password=${password}&source=android_app"
        conn.outputStream.use { it.write(params.toByteArray()) }

        val responseCode = conn.responseCode
        val response = if (responseCode == 200) {
            conn.inputStream.bufferedReader().readText()
        } else {
            conn.errorStream?.bufferedReader()?.readText() ?: "{\"ok\":false,\"error\":\"server_error\"}"
        }
        conn.disconnect()
        return response
    }

    private fun handleLoginResult(response: String, tenantId: String, username: String, password: String) {
        try {
            Log.d(TAG, "Login response: $response")
            val json = JSONObject(response)
            val ok = json.optBoolean("ok", false)

            if (ok) {
                val data = json.optJSONObject("data") ?: JSONObject()
                val token = data.optString("token", "")
                val userName = data.optString("name", username)
                val userId = data.optString("id", "")
                val userRole = data.optString("role", "employee")
                val tenantName = data.optString("tenant_name", tenantId)
                val isAdmin = data.optBoolean("is_admin", false)
                val branchId = data.optString("branch_id", "")
                val branchName = data.optString("branch_name", "")

                // Save session
                sessionManager.saveSession(
                    token = token,
                    userName = userName,
                    userId = userId,
                    userRole = userRole,
                    tenant = tenantName,
                    tenantId = tenantId,
                    isAdmin = isAdmin,
                    branchId = branchId,
                    branchName = branchName,
                    tenantName = tenantName
                )

                // Save credentials if remember me
                if (cbRemember.isChecked) {
                    sessionManager.saveCredentials(username, password)
                    sessionManager.saveTenantId(tenantId)
                } else {
                    sessionManager.clearSavedCredentials()
                }

                Toast.makeText(this, "مرحباً $userName", Toast.LENGTH_SHORT).show()
                goToMain()

            } else {
                val errorMsg = json.optString("error", "")
                val message = when (errorMsg) {
                    "unauthorized" -> "اسم المستخدم أو كلمة المرور غير صحيحة"
                    "tenant_not_found" -> "رقم المنشأة غير موجود"
                    "inactive" -> "الحساب غير مفعل"
                    else -> "فشل تسجيل الدخول: $errorMsg"
                }
                showError(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            showError("حدث خطأ في معالجة البيانات")
        }
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        btnLogin.text = if (loading) "جارٍ تسجيل الدخول..." else "تسجيل الدخول"
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

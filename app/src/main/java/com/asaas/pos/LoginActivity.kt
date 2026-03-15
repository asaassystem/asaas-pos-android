package com.asaas.pos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.asaas.pos.databinding.ActivityLoginBinding
import com.asaas.pos.utils.NetworkUtils
import com.asaas.pos.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Login Activity for ASAAS POS
 * Supports tenant login with saved credentials (Remember Me)
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val LOGIN_URL = "https://super.asaas-system.com/api/login.php"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Load saved credentials if Remember Me was set
        if (sessionManager.isRememberMe()) {
            binding.etUsername.setText(sessionManager.getSavedUsername())
            binding.etPassword.setText(sessionManager.getSavedPassword())
            val savedTenant = sessionManager.getTenantId()
            if (!savedTenant.isNullOrEmpty()) {
                binding.etTenant.setText(savedTenant)
            }
        }

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        // Settings button - navigate to settings activity
        try {
            binding.btnSettings.setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        } catch (e: Exception) {
            // btnSettings may not exist in all layout versions
        }
    }

    private fun performLogin() {
        val tenant = try { binding.etTenant.text.toString().trim() } catch (e: Exception) { "" }
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty()) {
            binding.etUsername.error = "يرجى إدخال اسم المستخدم"
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "يرجى إدخال كلمة المرور"
            return
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val formBody = FormBody.Builder()
                    .add("username", username)
                    .add("password", password)
                    .add("tenant_id", tenant)
                    .add("app_login", "true")
                    .add("device", "android")
                    .build()

                val request = Request.Builder()
                    .url(LOGIN_URL)
                    .post(formBody)
                    .addHeader("Accept", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    handleLoginResponse(body, username, password, tenant)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        this@LoginActivity,
                        "خطأ في الاتصال: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun handleLoginResponse(
        responseBody: String,
        username: String,
        password: String,
        tenant: String
    ) {
        try {
            val json = JSONObject(responseBody)
            val success = json.optBoolean("success", false)

            if (success) {
                val data = json.optJSONObject("data") ?: json
                val token = data.optString("token", "session_active")
                val userName = data.optString("user_name", username)
                val userId = data.optString("user_id", "")
                val userRole = data.optString("role", "employee")
                val tenantName = data.optString("tenant", tenant)
                val tenantId = data.optString("tenant_id", tenant)
                val isAdmin = data.optBoolean("is_admin", false)
                val branchId = data.optString("branch_id", "")
                val branchName = data.optString("branch_name", "")

                sessionManager.saveSession(
                    token = token,
                    userName = userName,
                    userId = userId,
                    userRole = userRole,
                    tenant = tenantName,
                    tenantId = tenantId,
                    isAdmin = isAdmin,
                    branchId = branchId,
                    branchName = branchName
                )

                // Always save credentials for auto-login (Remember Me)
                sessionManager.saveCredentials(username, password)

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                val message = json.optString("message", "اسم المستخدم أو كلمة المرور غير صحيحة")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في البيانات المستلمة", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.etUsername.isEnabled = !show
        binding.etPassword.isEnabled = !show
    }
}

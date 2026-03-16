package com.asaas.pos

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
        val t = sessionManager.getSavedTenantId()
        val u = sessionManager.getSavedUsername()
        if (!t.isNullOrEmpty()) etTenant.setText(t)
        if (!u.isNullOrEmpty()) etUsername.setText(u)
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
    }

    private fun doLogin(tenant: String, username: String, password: String) {
        setLoading(true)
        Executors.newSingleThreadExecutor().execute {
            try {
                val conn = URL(API_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("X-Requested-With", "AsaasPOS-Android")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val body = "action=login&tenant_id=${enc(tenant)}&username=${enc(username)}&password=${enc(password)}&source=android_app"
                conn.outputStream.use { it.write(body.toByteArray()) }
                val code = conn.responseCode
                val resp = if (code == 200) conn.inputStream.bufferedReader().readText()
                           else conn.errorStream?.bufferedReader()?.readText() ?: """{"ok":false,"error":"HTTP $code"}"""
                conn.disconnect()
                Log.d(TAG, "resp: $resp")
                runOnUiThread { setLoading(false); handleResponse(resp, tenant, username) }
            } catch (e: Exception) {
                Log.e(TAG, "err", e)
                runOnUiThread { setLoading(false); showError("Connection error") }
            }
        }
    }

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

    private fun handleResponse(resp: String, tenant: String, username: String) {
        try {
            val j = JSONObject(resp)
            if (j.optBoolean("ok", false)) {
                val d = j.optJSONObject("data") ?: JSONObject()
                sessionManager.saveSession(
                    authToken = d.optString("token", ""),
                    userName = username,
                    userId = d.optString("user_id", ""),
                    userRole = d.optString("role", "employee"),
                    tenant = tenant,
                    tenantName = d.optString("tenant_name", ""),
                    isAdmin = d.optBoolean("is_admin", false),
                    branchId = d.optString("branch_id", ""),
                    branchName = d.optString("branch_name", "")
                )
                sessionManager.saveCredentials(tenant, username)
                goToMain()
            } else {
                showError(j.optString("error", j.optString("message", "Login failed")))
            }
        } catch (e: Exception) {
            showError("Server error")
        }
    }

    private fun setLoading(b: Boolean) {
        progressBar.visibility = if (b) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !b
        etTenant.isEnabled = !b
        etUsername.isEnabled = !b
        etPassword.isEnabled = !b
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

package com.asaas.pos

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.asaas.pos.databinding.ActivitySettingsBinding
import com.asaas.pos.utils.SessionManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        loadCurrentSettings()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "اعدادات التطبيق"

        binding.btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnClearCache.setOnClickListener {
            clearAppCache()
        }
    }

    private fun loadCurrentSettings() {
        val tenant = sessionManager.getTenant() ?: ""
        binding.etServerUrl.setText("https://super.asaas-system.com")
        binding.etTenantId.setText(tenant)
        binding.tvUserName.text = "المستخدم: ${sessionManager.getUserName() ?: ""}"
        binding.tvUserRole.text = "الصلاحية: ${if (sessionManager.isAdmin()) "مدير" else "موظف"}"
    }

    private fun saveSettings() {
        val serverUrl = binding.etServerUrl.text.toString().trim()
        val tenantId = binding.etTenantId.text.toString().trim()

        if (serverUrl.isEmpty()) {
            binding.etServerUrl.error = "يرجى إدخال رابط الخادم"
            return
        }

        // Save to preferences
        getSharedPreferences("asaas_app_settings", MODE_PRIVATE).edit().apply {
            putString("server_url", serverUrl)
            putString("tenant_id", tenantId)
            apply()
        }

        Toast.makeText(this, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("تسجيل الخروج")
            .setMessage("هل تريد تسجيل الخروج من التطبيق؟")
            .setPositiveButton("نعم") { _, _ ->
                sessionManager.clearSession()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("لا", null)
            .show()
    }

    private fun clearAppCache() {
        try {
            // Clear WebView cache
            android.webkit.WebStorage.getInstance().deleteAllData()
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            
            Toast.makeText(this, "تم مسح الذاكرة المؤقتة", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في مسح الذاكرة", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

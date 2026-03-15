package com.asaas.pos.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * SessionManager - Manages tenant session data for ASAAS POS
 * Uses SharedPreferences with fallback safety
 */
class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        // Use regular SharedPreferences for reliability
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "asaas_pos_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_TENANT = "tenant"
        private const val KEY_TENANT_ID = "tenant_id"
        private const val KEY_TENANT_NAME = "tenant_name"
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_SAVED_USERNAME = "saved_username"
        private const val KEY_SAVED_PASSWORD = "saved_password"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_BRANCH_NAME = "branch_name"
    }

    fun saveSession(
        token: String,
        userName: String,
        userId: String,
        userRole: String,
        tenant: String,
        tenantId: String,
        isAdmin: Boolean,
        branchId: String = "",
        branchName: String = "",
        tenantName: String = ""
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_AUTH_TOKEN, token)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_ROLE, userRole)
            putString(KEY_TENANT, tenant)
            putString(KEY_TENANT_ID, tenantId)
            putString(KEY_TENANT_NAME, tenantName.ifEmpty { tenant })
            putBoolean(KEY_IS_ADMIN, isAdmin)
            putString(KEY_BRANCH_ID, branchId)
            putString(KEY_BRANCH_NAME, branchName)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)
    fun getToken(): String? = getAuthToken()  // alias
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUsername(): String? = getUserName()  // alias
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, "employee")
    fun getTenant(): String? = prefs.getString(KEY_TENANT, null)
    fun getTenantId(): String? = prefs.getString(KEY_TENANT_ID, null)
    fun getTenantName(): String? = prefs.getString(KEY_TENANT_NAME, null)
    fun isAdmin(): Boolean = prefs.getBoolean(KEY_IS_ADMIN, false)
    fun getBranchId(): String? = prefs.getString(KEY_BRANCH_ID, null)
    fun getBranchName(): String? = prefs.getString(KEY_BRANCH_NAME, null)

    fun saveCredentials(username: String, password: String) {
        prefs.edit().apply {
            putString(KEY_SAVED_USERNAME, username)
            putString(KEY_SAVED_PASSWORD, password)
            putBoolean(KEY_REMEMBER_ME, true)
            apply()
        }
    }

    fun saveTenantId(tenantId: String) {
        prefs.edit().putString(KEY_TENANT_ID, tenantId).apply()
    }

    fun clearSavedCredentials() {
        prefs.edit().apply {
            remove(KEY_SAVED_USERNAME)
            remove(KEY_SAVED_PASSWORD)
            putBoolean(KEY_REMEMBER_ME, false)
            apply()
        }
    }

    fun getSavedUsername(): String? = prefs.getString(KEY_SAVED_USERNAME, null)
    fun getSavedPassword(): String? = prefs.getString(KEY_SAVED_PASSWORD, null)
    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    fun logout() = clearSession()  // alias

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ID)
            remove(KEY_USER_ROLE)
            remove(KEY_TENANT)
            remove(KEY_TENANT_NAME)
            remove(KEY_IS_ADMIN)
            remove(KEY_BRANCH_ID)
            remove(KEY_BRANCH_NAME)
            apply()
        }
    }
}

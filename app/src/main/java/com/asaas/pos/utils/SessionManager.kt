package com.asaas.pos.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * SessionManager - Manages tenant session data for ASAAS POS
 * Uses SharedPreferences for reliability
 */
class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Separate prefs for saved login credentials
    private val credPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(CRED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_NAME = "asaas_pos_prefs"
        private const val CRED_PREFS_NAME = "asaas_pos_credentials"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_TENANT = "tenant"
        private const val KEY_TENANT_ID = "tenant_id"
        private const val KEY_TENANT_NAME = "tenant_name"
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_BRANCH_NAME = "branch_name"
        private const val KEY_SAVED_TENANT = "saved_tenant_id"
        private const val KEY_SAVED_USERNAME = "saved_username"
    }

    fun saveSession(
        authToken: String,
        userName: String,
        userId: String,
        userRole: String,
        tenant: String,
        tenantName: String = "",
        isAdmin: Boolean = false,
        branchId: String = "",
        branchName: String = ""
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_AUTH_TOKEN, authToken)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_ROLE, userRole)
            putString(KEY_TENANT, tenant)
            putString(KEY_TENANT_ID, tenant)
            putString(KEY_TENANT_NAME, tenantName.ifEmpty { tenant })
            putBoolean(KEY_IS_ADMIN, isAdmin)
            putString(KEY_BRANCH_ID, branchId)
            putString(KEY_BRANCH_NAME, branchName)
            apply()
        }
    }

    fun saveCredentials(tenantId: String, username: String) {
        credPrefs.edit().apply {
            putString(KEY_SAVED_TENANT, tenantId)
            putString(KEY_SAVED_USERNAME, username)
            apply()
        }
    }

    fun getSavedTenantId(): String? = credPrefs.getString(KEY_SAVED_TENANT, null)
    fun getSavedUsername(): String? = credPrefs.getString(KEY_SAVED_USERNAME, null)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)
    fun getToken(): String? = getAuthToken()
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUsername(): String? = getUserName()
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, "employee")
    fun getTenant(): String? = prefs.getString(KEY_TENANT, null)
    fun getTenantId(): String? = prefs.getString(KEY_TENANT_ID, null)
    fun getTenantName(): String? = prefs.getString(KEY_TENANT_NAME, null)
    fun isAdmin(): Boolean = prefs.getBoolean(KEY_IS_ADMIN, false)
    fun getBranchId(): String? = prefs.getString(KEY_BRANCH_ID, null)
    fun getBranchName(): String? = prefs.getString(KEY_BRANCH_NAME, null)

    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_AUTH_TOKEN)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}

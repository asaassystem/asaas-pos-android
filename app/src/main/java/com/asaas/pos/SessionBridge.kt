package com.asaas.pos

import android.webkit.JavascriptInterface
import com.asaas.pos.utils.SessionManager

/**
 * JavaScript bridge for session management
 * Called from WebView JS as: AndroidSession.getUserName()
 */
class SessionBridge(private val sessionManager: SessionManager) {

    @JavascriptInterface
    fun getUserName(): String = sessionManager.getUserName() ?: ""

    @JavascriptInterface
    fun getUserRole(): String = sessionManager.getUserRole() ?: "employee"

    @JavascriptInterface
    fun getUserId(): String = sessionManager.getUserId() ?: ""

    @JavascriptInterface
    fun getTenant(): String = sessionManager.getTenant() ?: ""

    @JavascriptInterface
    fun isAdmin(): Boolean = sessionManager.isAdmin()

    @JavascriptInterface
    fun logout() {
        sessionManager.clearSession()
    }

    @JavascriptInterface
    fun getAppVersion(): String = "1.0.0"

    @JavascriptInterface
    fun getPlatform(): String = "android"
}

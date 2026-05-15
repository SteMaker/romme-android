package com.romme.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import net.openid.appauth.*
import net.openid.appauth.ClientSecretPost

/**
 * Nextcloud OAuth2 Login via AppAuth.
 * Öffnet den Browser für den Nextcloud-Login und erhält ein Access-Token zurück.
 */
class NextcloudAuth(
    private val context: Context,
    private val nextcloudUrl: String,
    private val clientId: String,
    private val clientSecret: String,
    private val redirectUri: String = "com.romme://oauth2redirect"
) {
    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse("$nextcloudUrl/apps/oauth2/authorize"),
        Uri.parse("$nextcloudUrl/apps/oauth2/api/v1/token")
    )

    private val authService = AuthorizationService(context)

    /**
     * Erstellt einen Authorization Intent für den Browser-Login.
     */
    fun createAuthIntent(): Intent {
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            Uri.parse(redirectUri)
        ).setScope("openid").build()

        return authService.getAuthorizationRequestIntent(request)
    }

    /**
     * Verarbeitet die Antwort vom OAuth2 Redirect.
     * Tauscht den Authorization Code gegen ein Access-Token.
     */
    fun handleAuthResponse(
        intent: Intent,
        onSuccess: (accessToken: String) -> Unit,
        onError: (Exception) -> Unit,
        onCanceled: () -> Unit = {}
    ) {
        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        if (response == null && exception == null) {
            onCanceled()
            return
        }

        if (response != null) {
            val tokenRequest = response.createTokenExchangeRequest()
            val clientAuth = ClientSecretPost(clientSecret)
            authService.performTokenRequest(tokenRequest, clientAuth) { tokenResponse, tokenException ->
                if (tokenResponse != null) {
                    val accessToken = tokenResponse.accessToken
                    if (accessToken != null) {
                        onSuccess(accessToken)
                    } else {
                        onError(Exception("Kein Access-Token erhalten"))
                    }
                } else {
                    onError(tokenException ?: Exception("Token-Austausch fehlgeschlagen"))
                }
            }
        } else {
            onError(exception ?: Exception("Autorisierung fehlgeschlagen"))
        }
    }

    fun dispose() {
        authService.dispose()
    }
}

package com.boom.core.database

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.boom.core.model.AuthSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface SessionStorage {
    fun saveSession(session: AuthSession)
    fun getSession(): AuthSession?
    fun clearSession()
    fun getAccessToken(): String?
}

class AndroidSecureSessionStorage(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SessionStorage {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "boom_secure_session_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveSession(session: AuthSession) {
        val serialized = json.encodeToString(session)
        prefs.edit()
            .putString(KEY_SESSION_DATA, serialized)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .apply()
    }

    override fun getSession(): AuthSession? {
        val serialized = prefs.getString(KEY_SESSION_DATA, null) ?: return null
        return try {
            json.decodeFromString<AuthSession>(serialized)
        } catch (e: Exception) {
            null
        }
    }

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    override fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    companion object {
        private const val KEY_SESSION_DATA = "auth_session_data"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}

/**
 * In-memory session storage for testing and offline simulations
 */
class InMemorySessionStorage : SessionStorage {
    private var session: AuthSession? = null

    override fun saveSession(session: AuthSession) {
        this.session = session
    }

    override fun getSession(): AuthSession? = session

    override fun clearSession() {
        session = null
    }

    override fun getAccessToken(): String? = session?.accessToken
}

package com.kuts.klaf.networking.klafServer

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFERENCES_NAME = "klaf_pending_signup"
private const val PENDING_SIGNUP_KEY = "pending_signup_attempt"

class AndroidPendingSignUpAttemptStore(context: Context) : PendingSignUpAttemptStore {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): PendingSignUpAttempt? = preferences.getString(PENDING_SIGNUP_KEY, null)?.let { value ->
        Json.decodeFromString<PendingSignUpAttempt>(value)
    }

    override fun write(attempt: PendingSignUpAttempt) {
        check(preferences.edit().putString(PENDING_SIGNUP_KEY, Json.encodeToString(attempt)).commit()) {
            "Unable to persist pending sign-up"
        }
    }

    override fun clear() {
        check(preferences.edit().remove(PENDING_SIGNUP_KEY).commit()) {
            "Unable to clear pending sign-up"
        }
    }
}

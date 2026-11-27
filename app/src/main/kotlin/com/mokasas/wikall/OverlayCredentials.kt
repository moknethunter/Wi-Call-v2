package com.mokasas.wikall

import android.content.SharedPreferences

internal class OverlayCredentials(private val preferences: SharedPreferences) {
    private val revocation = Any()

    fun current(): Pair<String, Long> = synchronized(preferences) {
        val secret = preferences.getString("secret", null) ?: Wire.token().also {
            check(preferences.edit().putString("secret", it).commit())
        }
        secret to preferences.getLong("epoch", 0)
    }

    fun canSetPassphrase() = !preferences.contains("secret")

    fun setPassphrase(passphrase: String): Boolean = synchronized(preferences) {
        if (!canSetPassphrase()) return@synchronized false
        preferences.edit().putString("secret", newOverlaySecret(passphrase)).commit()
    }

    fun recoverInterruptedRevocation() = synchronized(revocation) {
        if (preferences.getBoolean("rekeyPending", false)) rotate()
    }

    /** Persist the intent before changing enrollment. A restart completes an interrupted rekey. */
    fun revokeDevice(revoke: () -> Unit): Boolean = synchronized(revocation) {
        check(preferences.edit().putBoolean("rekeyPending", true).commit())
        revoke()
        runCatching { rotate() }.isSuccess
    }

    private fun rotate() = synchronized(preferences) {
        val nextEpoch = Math.addExact(preferences.getLong("epoch", 0), 1)
        check(preferences.edit().putString("secret", Wire.token()).putLong("epoch", nextEpoch).remove("rekeyPending").commit())
    }
}

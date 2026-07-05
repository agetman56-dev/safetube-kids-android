package ua.safetube.kids.parental

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.time.LocalDate

private val Context.parentalDataStore by preferencesDataStore(name = "parental_prefs")

/**
 * PIN батьків (захищає налаштування й вихід із дитячого режиму), перемикач і ліміт часу
 * перегляду за день. Усе зберігається локально через DataStore — нічого не йде в мережу.
 */
class ParentalControls(private val context: Context) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val TIME_LIMIT_ENABLED = booleanPreferencesKey("time_limit_enabled")
        val TIME_LIMIT_MINUTES = intPreferencesKey("time_limit_minutes")
        val WATCHED_SECONDS = intPreferencesKey("watched_seconds_today")
        val WATCHED_DATE = stringPreferencesKey("watched_date")
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun isPinSet(): Boolean =
        context.parentalDataStore.data.map { it[Keys.PIN_HASH] != null }.first()

    suspend fun setPin(pin: String) {
        context.parentalDataStore.edit { it[Keys.PIN_HASH] = sha256(pin) }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = context.parentalDataStore.data.map { it[Keys.PIN_HASH] }.first()
        return stored != null && stored == sha256(pin)
    }

    val timeLimitEnabled: Flow<Boolean> =
        context.parentalDataStore.data.map { it[Keys.TIME_LIMIT_ENABLED] ?: false }

    suspend fun setTimeLimitEnabled(enabled: Boolean) {
        context.parentalDataStore.edit { it[Keys.TIME_LIMIT_ENABLED] = enabled }
    }

    val timeLimitMinutes: Flow<Int> =
        context.parentalDataStore.data.map { it[Keys.TIME_LIMIT_MINUTES] ?: DEFAULT_LIMIT_MINUTES }

    suspend fun setTimeLimitMinutes(minutes: Int) {
        context.parentalDataStore.edit { it[Keys.TIME_LIMIT_MINUTES] = minutes }
    }

    /** Секунди перегляду сьогодні; лічильник сам скидається, коли настає нова дата. */
    val watchedSecondsToday: Flow<Int> = context.parentalDataStore.data.map { prefs ->
        val today = LocalDate.now().toString()
        if (prefs[Keys.WATCHED_DATE] != today) 0 else prefs[Keys.WATCHED_SECONDS] ?: 0
    }

    suspend fun addWatchedSeconds(seconds: Int) {
        val today = LocalDate.now().toString()
        context.parentalDataStore.edit { prefs ->
            val current = if (prefs[Keys.WATCHED_DATE] == today) prefs[Keys.WATCHED_SECONDS] ?: 0 else 0
            prefs[Keys.WATCHED_DATE] = today
            prefs[Keys.WATCHED_SECONDS] = current + seconds
        }
    }

    companion object {
        const val DEFAULT_LIMIT_MINUTES = 60
    }
}

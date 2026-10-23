package com.libreta.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "libreta_prefs")

/** Widgets configurables de la pantalla de inicio. */
enum class HomeWidget(val label: String) {
    SCRATCH("Borrador rápido"),
    PINNED("Nota fijada"),
    RECENT("Notas recientes"),
    TASKS("Mis tareas"),
    SHORTCUTS("Atajos"),
    REMINDERS("Recordatorios"),
    NOTEBOOKS("Libretas"),
    TAGS("Etiquetas"),
}

data class Settings(
    val themeMode: Int = 0, // 0 sistema, 1 papel (claro), 2 pizarra (oscuro)
    val editorFont: Int = 0, // 0 sans, 1 mono, 2 serif
    val editorFontSize: Float = 16f,
    val paperStyle: Int = 1, // 0 liso, 1 rayado, 2 cuadriculado, 3 punteado
    val listView: Int = 1, // 0 lista, 1 tarjetas, 2 grilla, 3 compacta
    val sort: Int = 0, // 0 actualización, 1 creación, 2 título
    val defaultNotebookId: String? = null,
    val scratchpad: String = "",
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val biometric: Boolean = false,
    val lockTimeoutMin: Int = 0,
    val homeWidgets: List<HomeWidget> = HomeWidget.entries.toList(),
    val onboarded: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val trashAutoDays: Int = 30,
    val haptics: Boolean = true,
    val userName: String = "",
    val lastBackupAt: Long = 0,
) {
    val lockEnabled: Boolean get() = pinHash != null
}

class Prefs(private val context: Context) {
    private object K {
        val theme = intPreferencesKey("theme")
        val font = intPreferencesKey("font")
        val fontSize = floatPreferencesKey("font_size")
        val paper = intPreferencesKey("paper")
        val listView = intPreferencesKey("list_view")
        val sort = intPreferencesKey("sort")
        val defaultNb = stringPreferencesKey("default_nb")
        val scratch = stringPreferencesKey("scratch")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
        val biometric = booleanPreferencesKey("biometric")
        val lockTimeout = intPreferencesKey("lock_timeout")
        val homeWidgets = stringPreferencesKey("home_widgets")
        val onboarded = booleanPreferencesKey("onboarded")
        val recent = stringPreferencesKey("recent_searches")
        val trashDays = intPreferencesKey("trash_days")
        val haptics = booleanPreferencesKey("haptics")
        val userName = stringPreferencesKey("user_name")
        val lastBackup = longPreferencesKey("last_backup")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            themeMode = p[K.theme] ?: 0,
            editorFont = p[K.font] ?: 0,
            editorFontSize = p[K.fontSize] ?: 16f,
            paperStyle = p[K.paper] ?: 1,
            listView = p[K.listView] ?: 1,
            sort = p[K.sort] ?: 0,
            defaultNotebookId = p[K.defaultNb],
            scratchpad = p[K.scratch] ?: "",
            pinHash = p[K.pinHash],
            pinSalt = p[K.pinSalt],
            biometric = p[K.biometric] ?: false,
            lockTimeoutMin = p[K.lockTimeout] ?: 0,
            homeWidgets = p[K.homeWidgets]?.let { s ->
                s.split(',').mapNotNull { n -> HomeWidget.entries.firstOrNull { it.name == n } }
            } ?: HomeWidget.entries.toList(),
            onboarded = p[K.onboarded] ?: false,
            recentSearches = p[K.recent]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList(),
            trashAutoDays = p[K.trashDays] ?: 30,
            haptics = p[K.haptics] ?: true,
            userName = p[K.userName] ?: "",
            lastBackupAt = p[K.lastBackup] ?: 0L,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setTheme(v: Int) = context.dataStore.edit { it[K.theme] = v }
    suspend fun setFont(v: Int) = context.dataStore.edit { it[K.font] = v }
    suspend fun setFontSize(v: Float) = context.dataStore.edit { it[K.fontSize] = v }
    suspend fun setPaper(v: Int) = context.dataStore.edit { it[K.paper] = v }
    suspend fun setListView(v: Int) = context.dataStore.edit { it[K.listView] = v }
    suspend fun setSort(v: Int) = context.dataStore.edit { it[K.sort] = v }
    suspend fun setDefaultNotebook(id: String) = context.dataStore.edit { it[K.defaultNb] = id }
    suspend fun setScratchpad(v: String) = context.dataStore.edit { it[K.scratch] = v }
    suspend fun setBiometric(v: Boolean) = context.dataStore.edit { it[K.biometric] = v }
    suspend fun setLockTimeout(v: Int) = context.dataStore.edit { it[K.lockTimeout] = v }
    suspend fun setHomeWidgets(v: List<HomeWidget>) = context.dataStore.edit { it[K.homeWidgets] = v.joinToString(",") { w -> w.name } }
    suspend fun setOnboarded() = context.dataStore.edit { it[K.onboarded] = true }
    suspend fun setTrashDays(v: Int) = context.dataStore.edit { it[K.trashDays] = v }
    suspend fun setHaptics(v: Boolean) = context.dataStore.edit { it[K.haptics] = v }
    suspend fun setUserName(v: String) = context.dataStore.edit { it[K.userName] = v }
    suspend fun setLastBackup(v: Long) = context.dataStore.edit { it[K.lastBackup] = v }

    suspend fun addRecentSearch(q: String) = context.dataStore.edit { p ->
        val list = (listOf(q) + (p[K.recent]?.split('\n') ?: emptyList())).filter { it.isNotBlank() }.distinct().take(8)
        p[K.recent] = list.joinToString("\n")
    }

    suspend fun clearRecentSearches() = context.dataStore.edit { it.remove(K.recent) }

    suspend fun setPin(pin: String?) = context.dataStore.edit { p ->
        if (pin == null) {
            p.remove(K.pinHash); p.remove(K.pinSalt); p[K.biometric] = false
        } else {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
            p[K.pinSalt] = salt
            p[K.pinHash] = hash(pin, salt)
        }
    }

    fun checkPin(pin: String, s: Settings): Boolean =
        s.pinHash != null && s.pinSalt != null && hash(pin, s.pinSalt) == s.pinHash

    private fun hash(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((salt + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

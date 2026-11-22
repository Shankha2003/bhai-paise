package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppPreferences
import com.example.data.model.CurrencyOption
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sliceSplitDataStore: DataStore<Preferences> by preferencesDataStore(name = "slicesplit_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PAISE_PRECISION = booleanPreferencesKey("paise_precision")
        val CURRENCY = stringPreferencesKey("currency")
        val ONBOARDED = booleanPreferencesKey("has_completed_onboarding")
    }

    val preferences: Flow<AppPreferences> = context.sliceSplitDataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.LIGHT
        val paise = prefs[Keys.PAISE_PRECISION] ?: true
        val currency = prefs[Keys.CURRENCY]?.let {
            runCatching { CurrencyOption.valueOf(it) }.getOrNull()
        } ?: CurrencyOption.INR
        val onboarded = prefs[Keys.ONBOARDED] ?: false
        AppPreferences(
            themeMode = theme,
            paisePrecision = paise,
            currency = currency,
            hasCompletedOnboarding = onboarded,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.sliceSplitDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setPaisePrecision(enabled: Boolean) {
        context.sliceSplitDataStore.edit { it[Keys.PAISE_PRECISION] = enabled }
    }

    suspend fun setCurrency(currency: CurrencyOption) {
        context.sliceSplitDataStore.edit { it[Keys.CURRENCY] = currency.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.sliceSplitDataStore.edit { it[Keys.ONBOARDED] = completed }
    }
}

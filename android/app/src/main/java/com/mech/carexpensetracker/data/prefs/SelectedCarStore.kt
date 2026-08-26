package com.mech.carexpensetracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

class SelectedCarStore(private val context: Context) {
    private val selectedCarKey = stringPreferencesKey("selected_car_external_id")

    val selectedCarExternalId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[selectedCarKey]
    }

    suspend fun setSelectedCar(externalId: String?) {
        context.dataStore.edit { prefs ->
            if (externalId == null) {
                prefs.remove(selectedCarKey)
            } else {
                prefs[selectedCarKey] = externalId
            }
        }
    }
}

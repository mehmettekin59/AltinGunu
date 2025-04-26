package com.mehmettekin.altingunu.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mehmettekin.altingunu.di.drawResultsDataStore
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawResultsDataStore @Inject constructor(
    private val context: Context,
    moshi: Moshi
) {
    private val drawResultsKey = stringPreferencesKey("draw_results")
    private val participantsKey = stringPreferencesKey("participants")
    private val drawSettingsKey = stringPreferencesKey("draw_settings")

    private val drawResultsType = Types.newParameterizedType(
        List::class.java,
        DrawResult::class.java
    )

    private val participantsType = Types.newParameterizedType(
        List::class.java,
        Participant::class.java
    )

    private val drawResultsAdapter: JsonAdapter<List<DrawResult>> = moshi.adapter(drawResultsType)
    private val participantsAdapter: JsonAdapter<List<Participant>> = moshi.adapter(participantsType)
    private val drawSettingsAdapter: JsonAdapter<ParticipantsScreenWholeInformation> = moshi.adapter(ParticipantsScreenWholeInformation::class.java)

    // Draw Results operations
    suspend fun saveDrawResults(results: List<DrawResult>) {
        context.drawResultsDataStore.edit { preferences ->
            preferences[drawResultsKey] = drawResultsAdapter.toJson(results)
        }
    }

    suspend fun getDrawResults(): List<DrawResult> {
        return context.drawResultsDataStore.data.map { preferences ->
            val json = preferences[drawResultsKey] ?: "[]"
            drawResultsAdapter.fromJson(json) ?: emptyList()
        }.first()
    }

    suspend fun clearDrawResults() {
        context.drawResultsDataStore.edit { preferences ->
            preferences[drawResultsKey] = "[]"
        }
    }

    // Participants operations
    suspend fun saveParticipants(participants: List<Participant>) {

        context.drawResultsDataStore.edit { preferences ->
            preferences[participantsKey] = participantsAdapter.toJson(participants)
        }
    }

    suspend fun getParticipants(): List<Participant> {
        return context.drawResultsDataStore.data.map { preferences ->
            val json = preferences[participantsKey] ?: "[]"
            participantsAdapter.fromJson(json) ?: emptyList()
        }.first()
    }

    // Draw Settings operations
    suspend fun saveDrawSettings(settings: ParticipantsScreenWholeInformation) {
        context.drawResultsDataStore.edit { preferences ->
            preferences[drawSettingsKey] = drawSettingsAdapter.toJson(settings)
        }
    }

    suspend fun getDrawSettings(): ParticipantsScreenWholeInformation? {
        return context.drawResultsDataStore.data.map { preferences ->
            val json = preferences[drawSettingsKey] ?: return@map null
           drawSettingsAdapter.fromJson(json)

        }.first()
    }
}
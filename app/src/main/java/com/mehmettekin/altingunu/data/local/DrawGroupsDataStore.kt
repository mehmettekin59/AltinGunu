package com.mehmettekin.altingunu.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mehmettekin.altingunu.domain.model.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawGroupsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi
) {

    // ============ KEYS ============
    private val drawGroupsKey = stringPreferencesKey("draw_groups")

    // ============ JSON ADAPTERS ============
    private val drawGroupsType = Types.newParameterizedType(List::class.java, DrawGroup::class.java)
    private val drawGroupsAdapter: JsonAdapter<List<DrawGroup>> = moshi.adapter(drawGroupsType)

    // ============ DRAW GROUPS ============


    //Tüm çekiliş gruplarını Flow olarak döndürür

    fun getDrawGroupsFlow(): Flow<List<DrawGroup>> {
        return dataStore.data.map { preferences ->
            val json = preferences[drawGroupsKey] ?: "[]"
            drawGroupsAdapter.fromJson(json) ?: emptyList()
        }
    }

     // Tüm çekiliş gruplarını getirir
    suspend fun getDrawGroups(): List<DrawGroup> {
        return getDrawGroupsFlow().first()
    }


     // Belirli bir çekiliş grubunu getirir
    suspend fun getDrawGroupById(id: String): DrawGroup? {
        return getDrawGroups().find { it.id == id }
    }


    //Çekiliş gruplarını kaydeder
    suspend fun saveDrawGroups(groups: List<DrawGroup>) {
        dataStore.edit { preferences ->
            preferences[drawGroupsKey] = drawGroupsAdapter.toJson(groups)
        }
    }

    //Yeni çekiliş grubu ekler
    suspend fun addDrawGroup(group: DrawGroup) {
        val currentGroups = getDrawGroups().toMutableList()

        // Aynı ID'li grup varsa güncelle, yoksa ekle
        val existingIndex = currentGroups.indexOfFirst { it.id == group.id }
        if (existingIndex >= 0) {
            currentGroups[existingIndex] = group
        } else {
            currentGroups.add(group)
        }

        saveDrawGroups(currentGroups)
    }


    //Çekiliş grubunu günceller
    suspend fun updateDrawGroup(updatedGroup: DrawGroup) {
        val currentGroups = getDrawGroups().toMutableList()
        val index = currentGroups.indexOfFirst { it.id == updatedGroup.id }

        if (index >= 0) {
            currentGroups[index] = updatedGroup.copy(lastModifiedDate = System.currentTimeMillis())
            saveDrawGroups(currentGroups)
        }
    }

    //Çekiliş grubunu siler
    suspend fun deleteDrawGroup(id: String) {
        val currentGroups = getDrawGroups().toMutableList()
        currentGroups.removeAll { it.id == id }
        saveDrawGroups(currentGroups)
    }


     //Aktif çekiliş gruplarını getirir
    suspend fun getActiveDrawGroups(): List<DrawGroup> {
        return getDrawGroups().filter { it.isActive && !it.isCompleted }
    }


    //Tamamlanmış çekiliş gruplarını getirir
    suspend fun getCompletedDrawGroups(): List<DrawGroup> {
        return getDrawGroups().filter { it.isCompleted }
    }

    // ============ MIGRATION & CLEANUP ============


    //Eski çekiliş sisteminden veri migration
    suspend fun migrateLegacyData(
        legacySettings: ParticipantsScreenWholeInformation?,
        legacyParticipants: List<Participant>,
        legacyResults: List<DrawResult>
    ) {
        if (legacySettings != null && legacyParticipants.isNotEmpty()) {
            val legacyGroup = DrawGroup(
                id = "legacy_${System.currentTimeMillis()}",
                name = "Eski Çekiliş",
                description = "Otomatik olarak aktarılan eski çekiliş",
                settings = legacySettings,
                participants = legacyParticipants,
                results = legacyResults,
                isCompleted = legacyResults.isNotEmpty()
            )

            addDrawGroup(legacyGroup)
        }
    }


     //Tüm verileri temizler
    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.remove(drawGroupsKey)
        }
    }
}
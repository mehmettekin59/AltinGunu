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
    private val fcmTokensKey = stringPreferencesKey("fcm_tokens")
    private val invitationsKey = stringPreferencesKey("invitations")
    private val participationRequestsKey = stringPreferencesKey("participation_requests")

    // ============ JSON ADAPTERS ============
    private val drawGroupsType = Types.newParameterizedType(List::class.java, DrawGroup::class.java)
    private val fcmTokensType = Types.newParameterizedType(List::class.java, FcmToken::class.java)
    private val invitationsType = Types.newParameterizedType(List::class.java, DrawInvitation::class.java)
    private val participationRequestsType = Types.newParameterizedType(List::class.java, ParticipationRequest::class.java)

    private val drawGroupsAdapter: JsonAdapter<List<DrawGroup>> = moshi.adapter(drawGroupsType)
    private val fcmTokensAdapter: JsonAdapter<List<FcmToken>> = moshi.adapter(fcmTokensType)
    private val invitationsAdapter: JsonAdapter<List<DrawInvitation>> = moshi.adapter(invitationsType)
    private val participationRequestsAdapter: JsonAdapter<List<ParticipationRequest>> = moshi.adapter(participationRequestsType)

    // ============ DRAW GROUPS ============

    /**
     * Tüm çekiliş gruplarını Flow olarak döndürür
     */
    fun getDrawGroupsFlow(): Flow<List<DrawGroup>> {
        return dataStore.data.map { preferences ->
            val json = preferences[drawGroupsKey] ?: "[]"
            drawGroupsAdapter.fromJson(json) ?: emptyList()
        }
    }

    /**
     * Tüm çekiliş gruplarını getirir
     */
    suspend fun getDrawGroups(): List<DrawGroup> {
        return getDrawGroupsFlow().first()
    }

    /**
     * Belirli bir çekiliş grubunu getirir
     */
    suspend fun getDrawGroupById(id: String): DrawGroup? {
        return getDrawGroups().find { it.id == id }
    }

    /**
     * Çekiliş gruplarını kaydeder
     */
    suspend fun saveDrawGroups(groups: List<DrawGroup>) {
        dataStore.edit { preferences ->
            preferences[drawGroupsKey] = drawGroupsAdapter.toJson(groups)
        }
    }

    /**
     * Yeni çekiliş grubu ekler
     */
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

    /**
     * Çekiliş grubunu günceller
     */
    suspend fun updateDrawGroup(updatedGroup: DrawGroup) {
        val currentGroups = getDrawGroups().toMutableList()
        val index = currentGroups.indexOfFirst { it.id == updatedGroup.id }

        if (index >= 0) {
            currentGroups[index] = updatedGroup.copy(lastModifiedDate = System.currentTimeMillis())
            saveDrawGroups(currentGroups)
        }
    }

    /**
     * Çekiliş grubunu siler
     */
    suspend fun deleteDrawGroup(id: String) {
        val currentGroups = getDrawGroups().toMutableList()
        currentGroups.removeAll { it.id == id }
        saveDrawGroups(currentGroups)
    }

    /**
     * Aktif çekiliş gruplarını getirir
     */
    suspend fun getActiveDrawGroups(): List<DrawGroup> {
        return getDrawGroups().filter { it.isActive && !it.isCompleted }
    }

    /**
     * Tamamlanmış çekiliş gruplarını getirir
     */
    suspend fun getCompletedDrawGroups(): List<DrawGroup> {
        return getDrawGroups().filter { it.isCompleted }
    }

    // ============ FCM TOKENS ============

    /**
     * FCM token'larını getirir
     */
    suspend fun getFcmTokens(): List<FcmToken> {
        return dataStore.data.map { preferences ->
            val json = preferences[fcmTokensKey] ?: "[]"
            fcmTokensAdapter.fromJson(json) ?: emptyList()
        }.first()
    }

    /**
     * FCM token'larını kaydeder
     */
    suspend fun saveFcmTokens(tokens: List<FcmToken>) {
        dataStore.edit { preferences ->
            preferences[fcmTokensKey] = fcmTokensAdapter.toJson(tokens)
        }
    }

    /**
     * Yeni FCM token ekler
     */
    suspend fun addFcmToken(token: FcmToken) {
        val currentTokens = getFcmTokens().toMutableList()

        // Aynı participant için token varsa güncelle
        val existingIndex = currentTokens.indexOfFirst { it.participantId == token.participantId }
        if (existingIndex >= 0) {
            currentTokens[existingIndex] = token
        } else {
            currentTokens.add(token)
        }

        saveFcmTokens(currentTokens)
    }

    /**
     * Belirli bir grubun FCM token'larını getirir
     */
    suspend fun getGroupFcmTokens(groupId: String): List<FcmToken> {
        val group = getDrawGroupById(groupId) ?: return emptyList()
        val allTokens = getFcmTokens()

        return allTokens.filter { token ->
            group.participants.any { it.id == token.participantId }
        }
    }

    // ============ INVITATIONS ============

    /**
     * Davetleri getirir
     */
    suspend fun getInvitations(): List<DrawInvitation> {
        return dataStore.data.map { preferences ->
            val json = preferences[invitationsKey] ?: "[]"
            invitationsAdapter.fromJson(json) ?: emptyList()
        }.first()
    }

    /**
     * Davetleri kaydeder
     */
    suspend fun saveInvitations(invitations: List<DrawInvitation>) {
        dataStore.edit { preferences ->
            preferences[invitationsKey] = invitationsAdapter.toJson(invitations)
        }
    }

    /**
     * Yeni davet ekler
     */
    suspend fun addInvitation(invitation: DrawInvitation) {
        val currentInvitations = getInvitations().toMutableList()
        currentInvitations.add(invitation)
        saveInvitations(currentInvitations)
    }

    /**
     * Davet koduna göre davet getirir
     */
    suspend fun getInvitationByCode(inviteCode: String): DrawInvitation? {
        return getInvitations().find { it.inviteCode == inviteCode && it.isValid() }
    }

    // ============ PARTICIPATION REQUESTS ============

    /**
     * Katılım taleplerini getirir
     */
    suspend fun getParticipationRequests(): List<ParticipationRequest> {
        return dataStore.data.map { preferences ->
            val json = preferences[participationRequestsKey] ?: "[]"
            participationRequestsAdapter.fromJson(json) ?: emptyList()
        }.first()
    }

    /**
     * Katılım taleplerini kaydeder
     */
    suspend fun saveParticipationRequests(requests: List<ParticipationRequest>) {
        dataStore.edit { preferences ->
            preferences[participationRequestsKey] = participationRequestsAdapter.toJson(requests)
        }
    }

    /**
     * Yeni katılım talebi ekler
     */
    suspend fun addParticipationRequest(request: ParticipationRequest) {
        val currentRequests = getParticipationRequests().toMutableList()
        currentRequests.add(request)
        saveParticipationRequests(currentRequests)
    }

    /**
     * Bekleyen katılım taleplerini getirir
     */
    suspend fun getPendingRequests(groupId: String): List<ParticipationRequest> {
        return getParticipationRequests().filter {
            it.drawGroupId == groupId && it.status == ParticipationStatus.PENDING
        }
    }

    /**
     * Katılım talebini günceller
     */
    suspend fun updateParticipationRequest(requestId: String, status: ParticipationStatus) {
        val currentRequests = getParticipationRequests().toMutableList()
        val index = currentRequests.indexOfFirst { it.id == requestId }

        if (index >= 0) {
            currentRequests[index] = currentRequests[index].copy(status = status)
            saveParticipationRequests(currentRequests)
        }
    }

    // ============ MIGRATION & CLEANUP ============

    /**
     * Eski çekiliş sisteminden veri migration
     */
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

    /**
     * Tüm verileri temizler
     */
    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.remove(drawGroupsKey)
            preferences.remove(fcmTokensKey)
            preferences.remove(invitationsKey)
            preferences.remove(participationRequestsKey)
        }
    }
}
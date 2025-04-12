package com.mehmettekin.altingunu.domain.repository


import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.utils.ResultState
import kotlinx.coroutines.flow.Flow

/**
 * Çekiliş verileri, katılımcılar ve ayarlarla ilgili tüm veri işlemlerini yöneten repository arayüzü.
 */
interface DrawRepository {
    /**
     * Katılımcı listesini kaydeder.
     * @param participants Kaydedilecek katılımcılar listesi
     * @return İşlem sonucunu içeren ResultState
     */
    suspend fun saveParticipants(participants: List<Participant>): ResultState<Unit>

    /**
     * Kaydedilmiş katılımcı listesini getirir.
     * @return Katılımcılar listesini içeren ResultState
     */
    suspend fun getParticipants(): ResultState<List<Participant>>

    /**
     * Çekiliş ayarlarını kaydeder.
     * @param settings Kaydedilecek çekiliş ayarları
     * @return İşlem sonucunu içeren ResultState
     */
    suspend fun saveDrawSettings(settings: ParticipantsScreenWholeInformation): ResultState<Unit>

    /**
     * Kaydedilmiş çekiliş ayarlarını getirir.
     * @return Çekiliş ayarlarını içeren ResultState
     */
    suspend fun getDrawSettings(): ResultState<ParticipantsScreenWholeInformation?>

    /**
     * Çekiliş sonuçlarını kaydeder.
     * @param results Kaydedilecek çekiliş sonuçları listesi
     * @return İşlem sonucunu içeren ResultState
     */
    suspend fun saveDrawResults(results: List<DrawResult>): ResultState<Unit>

    /**
     * Kaydedilmiş çekiliş sonuçlarını getirir.
     * @return Çekiliş sonuçlarını içeren Flow<ResultState>
     */
    fun getDrawResults(): Flow<ResultState<List<DrawResult>>>

    /**
     * Tüm çekiliş sonuçlarını temizler.
     * @return İşlem sonucunu içeren ResultState
     */
    suspend fun clearDrawResults(): ResultState<Unit>
}
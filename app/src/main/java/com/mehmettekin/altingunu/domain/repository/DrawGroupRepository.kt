package com.mehmettekin.altingunu.domain.repository

import com.mehmettekin.altingunu.domain.model.*
import com.mehmettekin.altingunu.utils.ResultState
import kotlinx.coroutines.flow.Flow

//Çekiliş grupları ile ilgili tüm veri işlemlerini yöneten repository arayüzü

interface DrawGroupRepository {

    // ============ ÇOKLU ÇEKİLİŞ YÖNETİMİ ============


     //Tüm çekiliş gruplarını getirir
    fun getAllDrawGroups(): Flow<ResultState<List<DrawGroup>>>

    /**
     * Belirli bir çekiliş grubunu ID ile getirir
     */
    suspend fun getDrawGroupById(id: String): ResultState<DrawGroup?>

    /**
     * Yeni çekiliş grubu oluşturur
     */
    suspend fun createDrawGroup(drawGroup: DrawGroup): ResultState<String>

    /**
     * Çekiliş grubunu günceller
     */
    suspend fun updateDrawGroup(drawGroup: DrawGroup): ResultState<Unit>

    /**
     * Çekiliş grubunu siler
     */
    suspend fun deleteDrawGroup(id: String): ResultState<Unit>

    /**
     * Aktif çekiliş gruplarını getirir
     */
    fun getActiveDrawGroups(): Flow<ResultState<List<DrawGroup>>>

    /**
     * Tamamlanmış çekiliş gruplarını getirir
     */
    fun getCompletedDrawGroups(): Flow<ResultState<List<DrawGroup>>>

    // ============ ÖDEME TAKİBİ ============

    /**
     * Çekilişin ödeme durumunu günceller
     */
    suspend fun updatePaymentStatus(groupId: String, paymentIndex: Int): ResultState<Unit>

    /**
     * Çekilişi tamamlandı olarak işaretler
     */
    suspend fun markGroupAsCompleted(groupId: String): ResultState<Unit>

    // ============ LEGACY SUPPORT ============

    /**
     * Eski tek çekiliş sisteminden veri migration
     */
    suspend fun migrateLegacyData(): ResultState<Unit>
}
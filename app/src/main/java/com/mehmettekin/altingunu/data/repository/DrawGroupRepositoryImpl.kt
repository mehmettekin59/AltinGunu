package com.mehmettekin.altingunu.data.repository

import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.data.local.DrawGroupsDataStore
import com.mehmettekin.altingunu.data.local.DrawResultsDataStore
import com.mehmettekin.altingunu.domain.model.DrawGroup
import com.mehmettekin.altingunu.domain.repository.DrawGroupRepository
import com.mehmettekin.altingunu.notification.FirestoreService
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawGroupRepositoryImpl @Inject constructor(
    private val drawGroupsDataStore: DrawGroupsDataStore,
    private val legacyDrawResultsDataStore: DrawResultsDataStore ,
    private val firestoreService: FirestoreService
) : DrawGroupRepository {

    // ============ ÇOKLU ÇEKİLİŞ YÖNETİMİ ============

    override fun getAllDrawGroups(): Flow<ResultState<List<DrawGroup>>> {
        return drawGroupsDataStore.getDrawGroupsFlow().map { groups ->
            try {
                ResultState.Success(groups.sortedByDescending { it.lastModifiedDate })
            } catch (e: Exception) {
                ResultState.Error(
                    UiText.stringResource(
                        R.string.error_retrieving_draw_results,
                        e.message ?: ""
                    )
                )
            }
        }
    }

    override suspend fun updateDrawGroup(drawGroup: DrawGroup): ResultState<Unit> {
        return try {
            drawGroupsDataStore.updateDrawGroup(drawGroup)

            // ✅ Firebase'i de güncelle
            firestoreService.saveDrawGroup(drawGroup)

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }
    override suspend fun deleteDrawGroup(id: String): ResultState<Unit> {
        return try {
            // Local'den sil
            drawGroupsDataStore.deleteDrawGroup(id)

            // ✅ Firebase'den de sil
            firestoreService.deleteDrawGroup(id)

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_clearing_draw_results,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun getDrawGroupById(id: String): ResultState<DrawGroup?> {
        return try {
            val group = drawGroupsDataStore.getDrawGroupById(id)
            ResultState.Success(group)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_retrieving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun createDrawGroup(drawGroup: DrawGroup): ResultState<String> {
        return try {
            // ID oluştur
            val groupWithId = if (drawGroup.id.isEmpty()) {
                drawGroup.copy(id = UUID.randomUUID().toString())
            } else {
                drawGroup
            }

            drawGroupsDataStore.addDrawGroup(groupWithId)
            ResultState.Success(groupWithId.id)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }


    override fun getActiveDrawGroups(): Flow<ResultState<List<DrawGroup>>> {
        return flow {
            try {
                emit(ResultState.Loading)
                val activeGroups = drawGroupsDataStore.getActiveDrawGroups()
                emit(ResultState.Success(activeGroups.sortedByDescending { it.lastModifiedDate }))
            } catch (e: Exception) {
                emit(
                    ResultState.Error(
                        UiText.stringResource(
                            R.string.error_retrieving_draw_results,
                            e.message ?: ""
                        )
                    )
                )
            }
        }
    }

    override fun getCompletedDrawGroups(): Flow<ResultState<List<DrawGroup>>> {
        return flow {
            try {
                emit(ResultState.Loading)
                val completedGroups = drawGroupsDataStore.getCompletedDrawGroups()
                emit(ResultState.Success(completedGroups.sortedByDescending { it.lastModifiedDate }))
            } catch (e: Exception) {
                emit(
                    ResultState.Error(
                        UiText.stringResource(
                            R.string.error_retrieving_draw_results,
                            e.message ?: ""
                        )
                    )
                )
            }
        }
    }

    // ============ ÖDEME TAKİBİ ============

    override suspend fun updatePaymentStatus(groupId: String, paymentIndex: Int): ResultState<Unit> {
        return try {
            val group = drawGroupsDataStore.getDrawGroupById(groupId)
                ?: return ResultState.Error(UiText.stringResource(R.string.result_and_settings_are_not_found))

            val updatedGroup = group.copy(
                currentPaymentIndex = paymentIndex + 1,
                lastModifiedDate = System.currentTimeMillis()
            )

            drawGroupsDataStore.updateDrawGroup(updatedGroup)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun markGroupAsCompleted(groupId: String): ResultState<Unit> {
        return try {
            val group = drawGroupsDataStore.getDrawGroupById(groupId)
                ?: return ResultState.Error(UiText.stringResource(R.string.result_and_settings_are_not_found))

            val updatedGroup = group.copy(
                isCompleted = true,
                currentPaymentIndex = group.settings.durationMonths,
                lastModifiedDate = System.currentTimeMillis()
            )

            drawGroupsDataStore.updateDrawGroup(updatedGroup)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }

    // ============ LEGACY SUPPORT ============

    override suspend fun migrateLegacyData(): ResultState<Unit> {
        return try {
            // Legacy verilerini al
            val legacySettings = legacyDrawResultsDataStore.getDrawSettings()
            val legacyParticipants = legacyDrawResultsDataStore.getParticipants()
            val legacyResults = legacyDrawResultsDataStore.getDrawResults()

            // Eğer legacy veriler varsa migration yap
            if (legacySettings != null && legacyParticipants.isNotEmpty()) {
                drawGroupsDataStore.migrateLegacyData(
                    legacySettings = legacySettings,
                    legacyParticipants = legacyParticipants,
                    legacyResults = legacyResults
                )

                // Migration sonrası legacy verileri temizle (opsiyonel)
                legacyDrawResultsDataStore.clearDrawResults()
            }

            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }
}
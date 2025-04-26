package com.mehmettekin.altingunu.data.repository


import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.data.local.DrawResultsDataStore
import com.mehmettekin.altingunu.domain.model.DrawResult
import com.mehmettekin.altingunu.domain.model.Participant
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawRepositoryImpl @Inject constructor(
    private val drawResultsDataStore: DrawResultsDataStore
) : DrawRepository {

    override suspend fun saveParticipants(participants: List<Participant>): ResultState<Unit> {
        return try {
            drawResultsDataStore.saveParticipants(participants)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_participants,
                    e.message ?: ""
                )
            )
        }
    }



    override suspend fun getParticipants(): ResultState<List<Participant>> {
        return try {
            val participants = drawResultsDataStore.getParticipants()
            ResultState.Success(participants)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_retrieving_participants,
                    e.message ?: ""
                )
            )
        }
    }



    override suspend fun saveDrawSettings(settings: ParticipantsScreenWholeInformation): ResultState<Unit> {
        return try {
            drawResultsDataStore.saveDrawSettings(settings)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(UiText.stringResource(
                    R.string.error_saving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun getDrawSettings(): ResultState<ParticipantsScreenWholeInformation?> {
        return try {
            val settings = drawResultsDataStore.getDrawSettings()
            ResultState.Success(settings)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_retrieving_draw_settings,
                    e.message ?: ""
                )
            )
        }
    }

    override suspend fun saveDrawResults(results: List<DrawResult>): ResultState<Unit> {
        return try {
            drawResultsDataStore.saveDrawResults(results)
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(
                UiText.stringResource(
                    R.string.error_saving_draw_results,
                    e.message ?: ""
                )
            )
        }
    }

    override fun getDrawResults(): Flow<ResultState<List<DrawResult>>> {
        return flow {
            emit(ResultState.Loading)
            try {
                val results = drawResultsDataStore.getDrawResults()
                emit(ResultState.Success(results))
            } catch (e: Exception) {
                emit(
                    ResultState.Error(UiText.stringResource(
                        R.string.error_retrieving_draw_results,
                        e.message ?: ""
                    ))
                )
            }
        }
    }

    override suspend fun clearDrawResults(): ResultState<Unit> {
        return try {
            drawResultsDataStore.clearDrawResults()
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
}
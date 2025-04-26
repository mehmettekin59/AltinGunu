package com.mehmettekin.altingunu.data.repository

import android.content.Context
import android.util.Log
import com.mehmettekin.altingunu.R
import com.mehmettekin.altingunu.data.local.SettingsDataStore
import com.mehmettekin.altingunu.data.remote.KapaliCarsiApi
import com.mehmettekin.altingunu.domain.repository.KapaliCarsiRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import com.mehmettekin.altingunu.domain.model.ExchangeRate
import com.mehmettekin.altingunu.utils.NetworkUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map

@Singleton
class KapaliCarsiRepositoryImpl @Inject constructor(
    private val api: KapaliCarsiApi,
    private val settingsDataStore: SettingsDataStore,
    private val externalScope: CoroutineScope,
    @ApplicationContext private val context: Context
) : KapaliCarsiRepository {

    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 0)

    override fun manualRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getExchangeRates(): Flow<ResultState<List<ExchangeRate>>> {
        val refreshInterval = settingsDataStore.getApiUpdateInterval()
            .map { seconds -> seconds * 1000L }

        val automaticRefresh = refreshInterval.flatMapLatest {internalMs ->
            flow {
                while (true){
                    emit(Unit)
                    delay(internalMs)
                }
            }

        }

        // Manuel ve otomatik yenilemeyi birleştir.
        return merge(automaticRefresh, _refreshTrigger)
            .flatMapLatest {
                flow {
                    try {
                        emit(ResultState.Loading)
                        if (!NetworkUtils.isNetworkAvailable(context)) {
                            emit(ResultState.Error(UiText.stringResource(R.string.error_no_internet)))
                            return@flow
                        }
                        val rates = api.getExchangeRates()
                        emit(ResultState.Success(rates))
                    } catch (e: HttpException) {
                        emit(ResultState.Error(
                            e.response()?.errorBody()?.string()?.let { errorBody ->
                                UiText.dynamicString(errorBody)
                            } ?: UiText.stringResource(R.string.unexpected_error_occurred)
                        ))
                    } catch (e: IOException) {
                        emit(ResultState.Error(UiText.stringResource(R.string.couldnt_reach_server_no_internet_connection)))
                    } catch (e: Exception) {
                        emit(ResultState.Error(
                            e.localizedMessage?.let { UiText.dynamicString(it) }
                                ?: UiText.stringResource(R.string.error_unknown_error_occurred)
                        ))
                    }
                }
            }
            .shareIn(
                externalScope,
                SharingStarted.WhileSubscribed(5000),
                replay = 1
            )
    }
}
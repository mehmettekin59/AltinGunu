package com.mehmettekin.altingunu.data.repository

import com.mehmettekin.altingunu.data.remote.KapaliCarsiApi
import com.mehmettekin.altingunu.domain.repository.KapaliCarsiRepository
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

@Singleton
class KapaliCarsiRepositoryImpl @Inject constructor(
    private val api: KapaliCarsiApi
) : KapaliCarsiRepository {

    private val refreshInterval = 30000L
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 0)

    override fun manualRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getExchangeRates(): Flow<ResultState<List<ExchangeRate>>> {
        val automaticRefresh = flow {
            while (true) {
                emit(Unit)
                delay(refreshInterval)
            }
        }

        // Manuel ve otomatik yenilemeyi birleştir.
        return merge(automaticRefresh, _refreshTrigger)
            .flatMapLatest {
                flow {
                    try {
                        emit(ResultState.Loading)
                        val rates = api.getExchangeRates()
                        emit(ResultState.Success(rates))
                    } catch (e: HttpException) {
                        emit(ResultState.Error(UiText.dynamicString(e.response()?.errorBody()?.string() ?: "Unexpected error occurred")))
                    } catch (e: IOException) {
                        emit(ResultState.Error(UiText.dynamicString("Couldn't reach server. Check your internet connection.")))
                    } catch (e: Exception) {
                        emit(ResultState.Error(UiText.dynamicString(e.localizedMessage ?: "Unknown error occurred")))
                    }
                }
            }
            .shareIn(
                CoroutineScope(Dispatchers.IO + SupervisorJob()),
                SharingStarted.WhileSubscribed(5000),
                replay = 1
            )
    }
}
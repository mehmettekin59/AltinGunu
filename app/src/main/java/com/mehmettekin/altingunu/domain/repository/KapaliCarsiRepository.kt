package com.mehmettekin.altingunu.domain.repository

import com.mehmettekin.altingunu.domain.model.ExchangeRate
import com.mehmettekin.altingunu.utils.ResultState
import kotlinx.coroutines.flow.Flow

interface KapaliCarsiRepository {
    fun getExchangeRates(): Flow<ResultState<List<ExchangeRate>>>
    fun manualRefresh()
}
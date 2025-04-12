package com.mehmettekin.altingunu.data.remote

import com.mehmettekin.altingunu.domain.model.ExchangeRate
import retrofit2.http.GET

interface KapaliCarsiApi {
    @GET("/")
    suspend fun getExchangeRates(): List<ExchangeRate>
}
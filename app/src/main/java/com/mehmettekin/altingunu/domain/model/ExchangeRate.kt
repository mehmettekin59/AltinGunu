package com.mehmettekin.altingunu.domain.model

data class ExchangeRate(
    val code: String,
    val alis: String,
    val satis: String,
    val dusuk: Double,
    val yuksek: Double,
    val kapanis: Double,
    val tarih: String
)

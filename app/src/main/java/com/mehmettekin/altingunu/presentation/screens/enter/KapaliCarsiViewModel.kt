package com.mehmettekin.altingunu.presentation.screens.enter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.model.ExchangeRate
import com.mehmettekin.altingunu.domain.repository.KapaliCarsiRepository
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class KapaliCarsiViewModel @Inject constructor(
    private val repository: KapaliCarsiRepository
) : ViewModel() {

    // Ana state değişikliklerini tutan StateFlow
    private val _exchangeRates = MutableStateFlow<ResultState<List<ExchangeRate>>>(ResultState.Idle)
    val exchangeRates: StateFlow<ResultState<List<ExchangeRate>>> = _exchangeRates

    // Filtrelenmiş veriler için ayrı StateFlow'lar
    private val _goldRates = MutableStateFlow<List<ExchangeRate>>(emptyList())
    val goldRates = _goldRates.asStateFlow()

    private val _currencyRates = MutableStateFlow<List<ExchangeRate>>(emptyList())
    val currencyRates = _currencyRates.asStateFlow()

    // Saklanan tüm verilerin bir kopyası (Success durumunda güncellenir)
    private var currentDataList: List<ExchangeRate> = emptyList()

    init {
        observeExchangeRates()
    }

    /**
     * Manuel veri yenileme için method
     */
    fun refreshExchangeRates() {
        repository.manualRefresh()
    }

    /**
     * Repository'den gelen verileri izleyen method
     */
    private fun observeExchangeRates() {
        viewModelScope.launch {
            repository.getExchangeRates()
                .catch { error ->
                    // Hata durumunda state'i güncelle ama önceki verileri koru
                    _exchangeRates.value = ResultState.Error(
                        UiText.dynamicString(error.localizedMessage ?: "Unknown error")
                    )
                }
                .collectLatest { result ->
                    // Ana state'i güncelle
                    _exchangeRates.value = result

                    // Başarılı olursa, filtreli listeleri de güncelle
                    if (result is ResultState.Success) {
                        currentDataList = result.data
                        updateFilteredLists(result.data)
                    }
                }
        }
    }

    /**
     * Filtreli listeleri günceller
     * Bu fonksiyon state değişikliği yapacağı için sadece success durumunda çağrılmalı
     */
    private fun updateFilteredLists(data: List<ExchangeRate>) {
        viewModelScope.launch {
            // Filtreleri ayrı coroutine'lerde çalıştır
            val goldList = data.filter { it.code in Constraints.goldCodeList.toSet() }
            val currencyList = data.filter { it.code in Constraints.currencyCodeList.toSet() }

            // StateFlow'ları güncelle
            _goldRates.value = goldList
            _currencyRates.value = currencyList
        }
    }

    /**
     * ViewModel'de saklanan mevcut veri listesinden belirtilen koda sahip
     * ExchangeRate nesnesini bulur ve döndürür. State'i değiştirmez.
     *
     * @param code Bulunacak öğenin kodu (örn: "USDTRY", "CEYREK_YENI").
     * @return Eşleşen ExchangeRate nesnesi veya bulunamazsa/veri henüz yoksa null.
     */
    fun findExchangeRateByCode(code: String): ExchangeRate? {
        return currentDataList.find { it.code == code }
    }
}

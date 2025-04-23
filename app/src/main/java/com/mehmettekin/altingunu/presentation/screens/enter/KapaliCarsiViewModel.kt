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

    private val _exchangeRates = MutableStateFlow<ResultState<List<ExchangeRate>>>(ResultState.Idle)
    val exchangeRates: StateFlow<ResultState<List<ExchangeRate>>> = _exchangeRates

    // Saklanan tüm verilerin bir kopyası (Success durumunda güncellenir)
    // findExchangeRateByCode bunun üzerinden çalışacak
    private var currentDataList: List<ExchangeRate> = emptyList()

    init {
        observeExchangeRates()
    }

    // manuel yenileme için method
    fun refreshExchangeRates() {
        repository.manualRefresh()
    }

    private fun observeExchangeRates() {
        viewModelScope.launch {
            repository.getExchangeRates().collectLatest { result ->
                // Başarılı olursa, lokal listeyi de güncelle
                if (result is ResultState.Success) {
                    currentDataList = result.data
                } else if (result is ResultState.Error) {
                    // Hata durumunda lokal listeyi güncelleme seçeneği
                    // currentDataList = emptyList() // İstenirse aktifleştirilebilir
                }
                // Ana state'i güncelle
                _exchangeRates.value = result
            }
        }
    }

    /**
     * ViewModel'de saklanan mevcut veri listesinden belirtilen koda sahip
     * ExchangeRate nesnesini bulur ve döndürür. State'i değiştirmez.
     * API'den gelen son başarılı veri seti üzerinden arama yapar.
     *
     * @param code Bulunacak öğenin kodu (örn: "USDTRY", "CEYREK_YENI").
     * @return Eşleşen ExchangeRate nesnesi veya bulunamazsa/veri henüz yoksa null.
     */
    fun findExchangeRateByCode(code: String): ExchangeRate? {
        // Doğrudan saklanan `currentDataList` üzerinden arama yap
        // Bu, _exchangeRates'in Loading veya Error olduğu durumlarda bile
        // en son başarılı veriyi kullanmayı sağlar (isteğe bağlı bir davranış)
        // Eğer sadece Success durumunda aranması isteniyorsa:
        //return (_exchangeRates.value as? ResultState.Success)?.data?.find { it.code == code }
        return currentDataList.find { it.code == code }
    }
}



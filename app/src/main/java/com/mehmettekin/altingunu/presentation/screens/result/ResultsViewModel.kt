package com.mehmettekin.altingunu.presentation.screens.result

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.forEachIndexed

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val drawRepository: DrawRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ResultsState())
    val state: StateFlow<ResultsState> = _state.asStateFlow()

    init {
        loadResults()
        loadDrawSettings()
    }

    private fun loadResults() {
        viewModelScope.launch {
            drawRepository.getDrawResults().collectLatest { result ->
                when (result) {
                    is ResultState.Success -> {
                        _state.value = _state.value.copy(
                            results = result.data,
                            isLoading = false
                        )
                    }
                    is ResultState.Error -> {
                        _state.value = _state.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    is ResultState.Loading -> {
                        _state.value = _state.value.copy(
                            isLoading = true
                        )
                    }
                    is ResultState.Idle -> {
                        // No action needed
                    }
                }
            }
        }
    }

    private fun loadDrawSettings() {
        viewModelScope.launch {
            when (val result = drawRepository.getDrawSettings()) {
                is ResultState.Success -> {
                    _state.value = _state.value.copy(
                        drawSettings = result.data,
                        isLoading = false
                    )
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                else -> {
                    // No action needed for Loading and Idle
                }
            }
        }
    }

    fun createPdf(context: Context): Uri? {
        val results = _state.value.results
        val settings = _state.value.drawSettings

        if (results.isEmpty() || settings == null) {
            _state.value = _state.value.copy(
                error = UiText.dynamicString("Sonuçlar veya ayarlar bulunamadı")
            )
            return null
        }

        // Create a PDF document
        val document = PdfDocument()

        // Page info
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Set up paint for drawing
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        // Title
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw title
        canvas.drawText("Altın Günü Çekilişi Sonuçları", 50f, 50f, titlePaint)

        // Draw settings info
        val settingsInfoY = 80f
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // Format the item type
        val itemTypeText = when (settings.itemType) {
            ItemType.TL -> "TL"
            ItemType.CURRENCY -> "Döviz (${settings.specificItem})"
            ItemType.GOLD -> "Altın (${settings.specificItem})"
        }

        canvas.drawText("Değer Türü: $itemTypeText", 50f, settingsInfoY, paint)
        canvas.drawText("Aylık Miktar: ${settings.monthlyAmount}", 50f, settingsInfoY + 20, paint)
        canvas.drawText("Toplam Süre: ${settings.durationMonths} ay", 50f, settingsInfoY + 40, paint)
        canvas.drawText("Katılımcı Sayısı: ${settings.participantCount}", 50f, settingsInfoY + 60, paint)

        // Format current date
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Oluşturulma Tarihi: $currentDate", 50f, settingsInfoY + 80, paint)

        // Draw table header
        val headerY = settingsInfoY + 120
        val tableHeaderPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("Sıra", 50f, headerY, tableHeaderPaint)
        canvas.drawText("İsim", 100f, headerY, tableHeaderPaint)
        canvas.drawText("Ay", 250f, headerY, tableHeaderPaint)
        canvas.drawText("Miktar", 400f, headerY, tableHeaderPaint)

        // Draw horizontal line
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
        }
        canvas.drawLine(50f, headerY + 10, 550f, headerY + 10, linePaint)

        // Draw results
        var y = headerY + 40
        results.forEachIndexed { index, result ->
            canvas.drawText("${index + 1}", 50f, y, paint)
            canvas.drawText(result.participantName, 100f, y, paint)
            canvas.drawText(result.month, 250f, y, paint)
            canvas.drawText(result.amount, 400f, y, paint)

            y += 30

            // Add a page break if needed
            if (y > 800) {
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                val newPage = document.startPage(newPageInfo)
                canvas = newPage.canvas
                y = 50
            }
        }

        // Finish the page
        document.finishPage(page)

        try {
            // Create a file to write the PDF
            val fileName = "altin_gunu_cekilisi_${currentDate.replace("/", "")}.pdf"
            val file = File(context.filesDir, fileName)
            val fos = FileOutputStream(file)

            document.writeTo(fos)
            document.close()
            fos.close()

            // Get a content URI for the file
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            _state.value = _state.value.copy(
                pdfUri = uri
            )

            return uri
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                error = UiText.dynamicString("PDF oluşturulurken hata: ${e.message}")
            )
            document.close()
            return null
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(
            error = null
        )
    }

    fun clearPdfUri() {
        _state.value = _state.value.copy(
            pdfUri = null
        )
    }

    fun restart() {
        viewModelScope.launch {
            // Clear draw results
            drawRepository.clearDrawResults()

            // Reset state
            _state.value = ResultsState()
        }
    }
}
package com.mehmettekin.altingunu.presentation.screens.result

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmettekin.altingunu.domain.model.ExchangeRate
import com.mehmettekin.altingunu.domain.model.ItemType
import com.mehmettekin.altingunu.domain.model.ParticipantsScreenWholeInformation
import com.mehmettekin.altingunu.domain.repository.DrawRepository
import com.mehmettekin.altingunu.domain.repository.KapaliCarsiRepository
import com.mehmettekin.altingunu.ui.theme.NavyBlue
import com.mehmettekin.altingunu.utils.Constraints
import com.mehmettekin.altingunu.utils.ResultState
import com.mehmettekin.altingunu.utils.UiText
import com.mehmettekin.altingunu.utils.ValueFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject



@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val drawRepository: DrawRepository,
    private val kapaliCarsiRepository: KapaliCarsiRepository // Your ExchangeRateRepository

) : ViewModel() {

    // Keep the main state for UI elements and overall loading/errors
    private val _state = MutableStateFlow(ResultsState())
    val state: StateFlow<ResultsState> = _state.asStateFlow()

    // Keep exchange rates separate as they are fetched independently and used by both UI and PDF
    private val _exchangeRates = MutableStateFlow<ResultState<List<ExchangeRate>>>(ResultState.Idle)
    val exchangeRates: StateFlow<ResultState<List<ExchangeRate>>> = _exchangeRates

    init {
        loadResults()
        loadDrawSettings()
        loadExchangeRates() // <-- Start loading exchange rates when ViewModel is created
    }

    private fun loadResults() {
        viewModelScope.launch {
            drawRepository.getDrawResults().collectLatest { result ->
                when (result) {
                    is ResultState.Success -> {
                        _state.value = _state.value.copy(
                            results = result.data,
                            isLoading = false // Adjust loading state based on results only
                        )
                    }
                    is ResultState.Error -> {
                        _state.value = _state.value.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    is ResultState.Loading -> {
                        // Only set isLoading true if results are loading, keep it false for initial idle/success
                        if (_state.value.results.isEmpty()) { // Prevent full-screen loading if results are already shown
                            _state.value = _state.value.copy(isLoading = true)
                        }
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
                        // Keep isLoading state determined by results loading
                    )
                }
                is ResultState.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        // Keep isLoading state determined by results loading
                    )
                }
                else -> {
                    // No action needed for Loading and Idle
                }
            }
        }
    }

    // Load exchange rates into the separate StateFlow
    private fun loadExchangeRates() {
        viewModelScope.launch {
            kapaliCarsiRepository.getExchangeRates().collectLatest { result ->
                _exchangeRates.value = result
                // Do not update the main isLoading based on exchange rates loading,
                // as the main content can be shown while rates are loading.
            }
        }
    }


    private fun getItemTypeText(settings: ParticipantsScreenWholeInformation): String {
        return when (settings.itemType) {
            ItemType.TL -> "TL"
            ItemType.CURRENCY -> {
                val currencyName = Constraints.currencyCodeToName[settings.specificItem] ?: settings.specificItem
                "Döviz ($currencyName)"
            }
            ItemType.GOLD -> {
                val goldName = Constraints.goldCodeToName[settings.specificItem] ?: settings.specificItem
                "Altın ($goldName)"
            }
        }
    }


    fun createPdf(context: Context): Uri? {
        val results = _state.value.results
        val settings = _state.value.drawSettings
        val exchangeRatesState = _exchangeRates.value // <-- Access the current value of exchange rates

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
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

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

        val amountPaint = Paint().apply {
            color = NavyBlue.toArgb()
            textSize = 12f //  Metin boyutu
            textAlign = Paint.Align.CENTER // MERKEZE HİZALA
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Kalın yapmak isterseniz
        }
        // Draw title
        canvas.drawText("Altın Günü Çekilişi Sonuçları", 50f, 50f, titlePaint)

        // Draw settings info
        var currentY = 80f // Start Y position for settings
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // Draw fixed settings
        val itemTypeText = getItemTypeText(settings) // Use the helper function
        canvas.drawText("Değer Türü:", 50f, currentY, paint)
        canvas.drawText(itemTypeText, 250f, currentY, paint) // Adjust X position for value
        currentY += 20

        val formattedMonthlyAmount = ValueFormatter.formatWithSymbol(
            settings.calculateAmountPerPerson().toString(),
            settings.itemType,
            settings.specificItem
        )
        canvas.drawText("Aylık Miktar:", 50f, currentY, paint)
        canvas.drawText(formattedMonthlyAmount, 250f, currentY, paint) // Adjust X position
        currentY += 20

        canvas.drawText("Toplam Süre:", 50f, currentY, paint)
        canvas.drawText("${settings.durationMonths} ay", 250f, currentY, paint) // Adjust X position
        currentY += 20

        canvas.drawText("Katılımcı Sayısı:", 50f, currentY, paint)
        canvas.drawText("${settings.participantCount}", 250f, currentY, paint) // Adjust X position
        currentY += 20

        // Add Current Unit Price if applicable <-- NEW
        val specificItemCode = settings.specificItem
        if ((settings.itemType == ItemType.CURRENCY || settings.itemType == ItemType.GOLD)
            && specificItemCode.isNotBlank()
        ) {
            canvas.drawText("Güncel Birim Fiyat:", 50f, currentY, paint)

            val currentUnitValueText = when (exchangeRatesState) {
                is ResultState.Success -> {
                    val rate = exchangeRatesState.data.find { it.code == specificItemCode }
                    if (rate != null) {
                        // Use the rate's sales price for display
                        val formattedValue = ValueFormatter.formatWithSymbol(rate.satis, settings.itemType, specificItemCode)
                        "$formattedValue TL"
                    } else {
                        "Fiyat bulunamadı"
                    }
                }
                is ResultState.Loading -> "Yükleniyor..."
                is ResultState.Error -> "Alınamadı"
                is ResultState.Idle -> "Bekleniyor..."
            }
            canvas.drawText(currentUnitValueText, 250f, currentY, paint) // Adjust X position for value
            currentY += 20 // Move down for the next item
        }

        // Format current date
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Oluşturulma Tarihi:", 50f, currentY, paint)
        canvas.drawText(currentDate, 250f, currentY, paint) // Adjust X position
        currentY += 40 // Add extra space before the table

        // Draw table header
        val headerY = currentY // Table header starts after settings and date
        val tableHeaderPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Adjusted X positions for table headers to align with columns
        canvas.drawText("Sıra", 50f, headerY, tableHeaderPaint)
        canvas.drawText("İsim", 120f, headerY, tableHeaderPaint) // Shifted right slightly
        canvas.drawText("Ay", 350f, headerY, tableHeaderPaint) // Shifted right
        canvas.drawText("Miktar", 500f, headerY, tableHeaderPaint) // Shifted right

        // Draw horizontal line
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
        }

        canvas.drawLine(50f, headerY + 10, 550f, headerY + 10, linePaint)

        // Draw results table content
        var y = headerY + 40 // Start Y position for table rows
        results.forEachIndexed { index, result ->
            // Check if a new page is needed *before* drawing the row
            if (y > 800) { // Check against lower bound of page
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                page = document.startPage(newPageInfo)
                canvas = page.canvas
                y = 50.toFloat() // Reset Y for the new page
                // Optionally redraw table header on new page
                // canvas.drawText("Sıra", 50f, y, tableHeaderPaint)
                // canvas.drawText("İsim", 120f, y, tableHeaderPaint)
                // canvas.drawText("Ay", 350f, y, tableHeaderPaint)
                // canvas.drawText("Miktar", 450f, y, tableHeaderPaint)
                // y += 40 // Add space after header
            }

            // Adjusted X positions for table content
            canvas.drawText("${index + 1}", 50f, y, paint)
            canvas.drawText(result.participantName, 120f, y, paint)
            canvas.drawText(result.month, 350f, y, paint)
            // Use result.amount which is the stored monthly value
            canvas.drawText(result.amount, 500f, y, amountPaint)

            y += 30 // Move down for the next row
        }

        // Finish the last page
        document.finishPage(page)

        try {
            // Create a temporary file in cacheDir or filesDir
            val fileName = "altin_gunu_cekilisi_${currentDate.replace("/", "")}.pdf"
            val tempFile = File(context.filesDir, fileName) // Use filesDir

            val fos = FileOutputStream(tempFile)

            document.writeTo(fos)
            document.close()
            fos.close()

            // Get a content URI for the temporary file (for viewing/sharing)
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)

            _state.value = _state.value.copy(
                pdfUri = uri
            )

            return uri // Return the URI for viewing/sharing
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                error = UiText.dynamicString("PDF oluşturulurken hata: ${e.message}")
            )
            document.close()
            return null
        }
    }

    // PDF'yi indirilenler klasörüne kaydetme
    fun savePdfToDownloads(context: Context) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)

                // Önce PDF'i geçici bir dosyaya oluştur ve URI'sini al.
                // Bu fonksiyon aynı zamanda _state.value.pdfUri'yi de günceller.
                val tempPdfUri = createPdf(context)

                // Geçici dosyayı silmek için File nesnesini oluşturacağız.
                // lastPathSegment null olabileceğinden null kontrolü yapmalıyız.
                val tempFileNameForCleanup = tempPdfUri?.lastPathSegment
                val tempFileForCleanup = if (tempFileNameForCleanup != null) File(context.filesDir, tempFileNameForCleanup) else null


                if (tempPdfUri != null) {
                    // İndirilenler klasörü için dosya adı oluştur (Bu, hedef dosyanın adıdır)
                    val destFileName = "altin_gunu_cekilisi_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ için MediaStore API kullan
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, destFileName) // Hedef dosya adını kullan
                            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // Doğrudan Downloads'a kaydet
                            put(MediaStore.Downloads.IS_PENDING, 1) // Beklemede olarak işaretle
                        }

                        val contentResolver = context.contentResolver
                        val downloadUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                        val pdfCollectionUri = contentResolver.insert(downloadUri, contentValues)

                        if (pdfCollectionUri != null) {
                            // Geçici dosyanın URI'sinden içeriği oku ve Downloads hedef URI'sine kopyala
                            context.contentResolver.openInputStream(tempPdfUri)?.use { inputStream -> // tempPdfUri'den doğrudan oku
                                contentResolver.openOutputStream(pdfCollectionUri)?.use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }

                            // Dosyayı "beklemede değil" olarak işaretle
                            contentValues.clear()
                            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                            contentResolver.update(pdfCollectionUri, contentValues, null, null)

                            // Geçici dosyayı sil
                            tempFileForCleanup?.delete()


                            _state.value = _state.value.copy(
                                isLoading = false,
                                message = UiText.dynamicString("PDF indirilenler klasörüne kaydedildi: $destFileName")
                            )
                        } else {
                            // Kaydetme başarısız olursa geçici dosyayı silmeye çalış
                            tempFileForCleanup?.delete()

                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = UiText.dynamicString("Dosya oluşturulamadı (Android 10+)")
                            )
                        }
                    } else {
                        // Android 9 ve altı için doğrudan Downloads klasörüne kaydet
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        downloadsDir.mkdirs() // Dizinin var olduğundan emin ol
                        val destinationFile = File(downloadsDir, destFileName) // Hedef dosya adını kullan

                        // Geçici dosyanın URI'sinden içeriği oku ve hedef dosyaya kopyala
                        context.contentResolver.openInputStream(tempPdfUri)?.use { inputStream -> // tempPdfUri'den doğrudan oku
                            FileOutputStream(destinationFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        // Geçici dosyayı sil
                        tempFileForCleanup?.delete()


                        // MediaStore'u güncelle (eski Android'lerde dosyanın görünmesi için gerekli)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(destinationFile.absolutePath),
                            arrayOf("application/pdf"),
                            null
                        )

                        _state.value = _state.value.copy(
                            isLoading = false,
                            message = UiText.dynamicString("PDF indirilenler klasörüne kaydedildi: $destFileName")
                        )
                    }
                } else {
                    // createPdf başarısız oldu, hata mesajı orada zaten ayarlandı
                    _state.value = _state.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = UiText.dynamicString("PDF kaydedilirken hata: ${e.message}")
                )
            }
        }
    }

    // Mesaj bildirimini temizle
    fun dismissMessage() {
        _state.value = _state.value.copy(message = null)
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

            // Reset state and reload initial data
            _state.value = ResultsState()
            _exchangeRates.value = ResultState.Idle // Reset exchange rates state
            loadResults()
            loadDrawSettings()
            loadExchangeRates() // Reload exchange rates
        }
    }
}
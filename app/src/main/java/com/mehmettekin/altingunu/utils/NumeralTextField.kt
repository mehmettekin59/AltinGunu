package com.mehmettekin.altingunu.utils



import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumeralTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    // Display value - kullanıcının gördüğü değer
    var displayValue by remember { mutableStateOf("") }

    // Value değiştiğinde display value'yu dil ayarına göre güncelle
    LaunchedEffect(value) {
        displayValue = value.convertNumerals()
    }

    OutlinedTextField(
        value = displayValue,
        onValueChange = { newDisplayValue ->
            // Kullanıcı girdisini normalize et (Latin rakamlar)
            val normalizedValue = NumeralHelper.normalizeInput(newDisplayValue)

            // Sayı formatını kontrol et
            if (normalizedValue.isEmpty() || normalizedValue.toDoubleOrNull() != null) {
                // Display value'yu güncelle - anında görsel değişiklik için
                displayValue = normalizedValue.convertNumerals()
                // Normalize edilmiş değeri geri döndür (state'e kaydedilecek)
                onValueChange(normalizedValue)
            }
        },
        label = {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = TextStyle(
                    textDirection = RTLHelper.getTextDirection()
                )
            )
        },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = leadingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
        ),
        textStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textDirection = RTLHelper.getTextDirection(),
            textAlign = RTLHelper.getStartTextAlign()
        ),
        shape = RoundedCornerShape(8.dp),
        singleLine = true
    )
}
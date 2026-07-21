package com.tejashaqua.app.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val indianLocale = Locale("en", "IN")
    private val numberFormat = NumberFormat.getNumberInstance(indianLocale)

    fun formatPrice(value: Any?): String {
        if (value == null) return ""
        val amount = when (value) {
            is Number -> value.toDouble()
            is String -> value.replace(",", "").toDoubleOrNull()
            else -> null
        } ?: return value.toString()
        
        return numberFormat.format(amount)
    }

    class IndianNumberVisualTransformation : VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val originalText = text.text
            if (originalText.isEmpty()) {
                return TransformedText(text, OffsetMapping.Identity)
            }

            // Only format if it's a valid number
            if (!originalText.replace(".", "").all { it.isDigit() }) {
                return TransformedText(text, OffsetMapping.Identity)
            }

            val formattedText = formatIndian(originalText)
            
            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0
                    val safeOffset = offset.coerceAtMost(originalText.length)
                    val originalPart = originalText.substring(0, safeOffset)
                    val transformedPart = formatIndian(originalPart)
                    return transformedPart.length
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 0) return 0
                    val safeOffset = offset.coerceAtMost(formattedText.length)
                    val transformedPart = formattedText.substring(0, safeOffset)
                    return transformedPart.replace(",", "").length
                }
            }

            return TransformedText(AnnotatedString(formattedText), offsetMapping)
        }

        private fun formatIndian(value: String): String {
            val cleanValue = value.replace(",", "")
            if (cleanValue.isEmpty()) return ""
            
            val parts = cleanValue.split(".")
            val integerPart = parts[0]
            val decimalPart = if (parts.size > 1) "." + parts[1] else ""

            if (integerPart.length <= 3) {
                return integerPart + decimalPart
            }

            val lastThree = integerPart.substring(integerPart.length - 3)
            val rest = integerPart.substring(0, integerPart.length - 3)
            
            val sb = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                sb.append(rest[i])
                count++
                if (count == 2 && i > 0) {
                    sb.append(",")
                    count = 0
                }
            }
            
            return sb.reverse().toString() + "," + lastThree + decimalPart
        }
    }
}

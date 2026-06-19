package com.tejashaqua.app.utils

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
}

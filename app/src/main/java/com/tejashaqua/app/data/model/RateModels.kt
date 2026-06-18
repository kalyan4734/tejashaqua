package com.tejashaqua.app.data.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.tejashaqua.app.R

data class AquaRate(
    val name: String = "",
    val price: String = "",
    val change: String = "",
    val trend: RateTrend = RateTrend.FLAT,
    val isPrawn: Boolean = false,
    val lastUpdated: Long = 0L
) {
    val icon: Int
        get() = when (name.lowercase(java.util.Locale.ROOT)) {
            "rohu" -> R.drawable.fish
            "prawns" -> R.drawable.prawn
            else -> if (isPrawn) R.drawable.prawn else R.drawable.fish
        }
        
    val iconBgColor: Color
        get() = when (name.lowercase(java.util.Locale.ROOT)) {
            "rohu" -> Color(0xFFFFF3E0)
            "katla", "prawns" -> Color(0xFFE0F2F1)
            "tilapia" -> Color(0xFFE8EAF6)
            "pangasius" -> Color(0xFFFCE4EC)
            else -> Color(0xFFF5F5F5)
        }

    @Composable
    fun getDisplayName(): String {
        return when (name.lowercase(java.util.Locale.ROOT)) {
            "rohu" -> stringResource(R.string.fish_rohu)
            "katla" -> stringResource(R.string.fish_katla)
            "karamosu" -> stringResource(R.string.fish_karamosu)
            "gaddi chepa" -> stringResource(R.string.fish_gaddi_chepa)
            "pangasius" -> stringResource(R.string.fish_pangasius)
            "roopchand" -> stringResource(R.string.fish_roopchand)
            "pandu gappa" -> stringResource(R.string.fish_pandu_gappa)
            "tilapia" -> stringResource(R.string.fish_tilapia)
            "chitala" -> stringResource(R.string.fish_chitala)
            "koramenu" -> stringResource(R.string.fish_koramenu)
            "valuga" -> stringResource(R.string.fish_valuga)
            "engilayi" -> stringResource(R.string.fish_engilayi)
            "jalla" -> stringResource(R.string.fish_jalla)
            "tuna" -> stringResource(R.string.fish_tuna)
            "pulasa" -> stringResource(R.string.fish_pulasa)
            "crab" -> stringResource(R.string.fish_crab)
            "others" -> stringResource(R.string.fish_others)
            "prawns" -> stringResource(R.string.cat_prawns)
            else -> name
        }
    }
}

enum class RateTrend {
    UP, DOWN, FLAT
}

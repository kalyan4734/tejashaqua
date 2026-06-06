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
        get() = when (name) {
            "Rohu" -> R.drawable.fish
            "Prawns" -> R.drawable.prawn
            else -> if (isPrawn) R.drawable.prawn else R.drawable.fish
        }
        
    val iconBgColor: Color
        get() = when (name) {
            "Rohu" -> Color(0xFFFFF3E0)
            "Katla", "Prawns" -> Color(0xFFE0F2F1)
            "Tilapia" -> Color(0xFFE8EAF6)
            "Pangasius" -> Color(0xFFFCE4EC)
            else -> Color(0xFFF5F5F5)
        }

    @Composable
    fun getDisplayName(): String {
        return when (name) {
            "Rohu" -> stringResource(R.string.fish_rohu)
            "Katla" -> stringResource(R.string.fish_katla)
            "Karamosu" -> stringResource(R.string.fish_karamosu)
            "Gaddi chepa" -> stringResource(R.string.fish_gaddi_chepa)
            "Pangasius" -> stringResource(R.string.fish_pangasius)
            "Roopchand" -> stringResource(R.string.fish_roopchand)
            "Pandu gappa" -> stringResource(R.string.fish_pandu_gappa)
            "Tilapia" -> stringResource(R.string.fish_tilapia)
            "Chitala" -> stringResource(R.string.fish_chitala)
            "Koramenu" -> stringResource(R.string.fish_koramenu)
            "Valuga" -> stringResource(R.string.fish_valuga)
            "Engilayi" -> stringResource(R.string.fish_engilayi)
            "Jalla" -> stringResource(R.string.fish_jalla)
            "Tuna" -> stringResource(R.string.fish_tuna)
            "Pulasa" -> stringResource(R.string.fish_pulasa)
            "Crab" -> stringResource(R.string.fish_crab)
            "Others" -> stringResource(R.string.fish_others)
            "Prawns" -> stringResource(R.string.cat_prawns)
            else -> name
        }
    }
}

enum class RateTrend {
    UP, DOWN, FLAT
}

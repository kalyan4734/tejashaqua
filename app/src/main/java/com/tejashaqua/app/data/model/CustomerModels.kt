package com.tejashaqua.app.data.model

data class CustomerInfo(
    val name: String,
    val mobile: String,
    val pan: String,
    val aadhar: String,
    val address: String,
    val about: String,
    val imageUrl: String? = null
)

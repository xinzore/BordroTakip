package com.bordrotakip.calendar

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String?,
    val accountType: String?
) {
    fun getDisplayLabel(): String {
        val account = accountName?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
        return "$displayName$account"
    }
}


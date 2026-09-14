package com.tejashaqua.app.utils

object AppStateTracker {
    var activeChatId: String? = null
    var isAppInForeground: Boolean = false
    val viewedListingIds = mutableSetOf<String>()
}

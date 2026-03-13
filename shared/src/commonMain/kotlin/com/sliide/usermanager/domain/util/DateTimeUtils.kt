package com.sliide.usermanager.domain.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object DateTimeUtils {
    fun formatRelativeTime(epochMs: Long): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val diffSeconds = (now - epochMs) / 1000

        return when {
            diffSeconds < 60 -> "Just now"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
            else -> "${diffSeconds / 86400}d ago"
        }
    }
}

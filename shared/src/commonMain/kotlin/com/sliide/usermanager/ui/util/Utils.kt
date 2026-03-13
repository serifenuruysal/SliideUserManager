package com.sliide.usermanager.ui.util

import kotlinx.datetime.Clock

// Fix #13 — Regex compiled at class-load time, not on every keystroke.
// isValidEmail() is called on every character typed in AddUserSheet; creating
// a new Regex object each call allocates on the hot path and triggers GC pressure.
private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

/**
 * Converts an epoch-millisecond timestamp to a human-readable relative string.
 * Pure Kotlin — no platform dependencies, fully unit-testable.
 */
fun Long.toRelativeTimeString(
    now: Long = Clock.System.now().toEpochMilliseconds()
): String {
    val diff = now - this
    return when {
        diff < 0          -> "Just now"
        diff < 60_000     -> "Just now"
        diff < 3_600_000  -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000-> "${diff / 86_400_000}d ago"
        else              -> "${diff / 604_800_000}w ago"
    }
}

/** RFC-compatible email validator. Pre-compiled — safe to call on every keystroke. */
fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)

/** Name must contain at least 2 non-whitespace characters. */
fun isValidName(name: String): Boolean = name.trim().length >= 2

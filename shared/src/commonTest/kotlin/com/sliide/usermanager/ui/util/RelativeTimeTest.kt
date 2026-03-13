package com.sliide.usermanager.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {

    private val baseNow = 1_000_000L

    @Test fun `just now for zero diff`() =
        assertEquals("Just now", baseNow.toRelativeTimeString(baseNow))

    @Test fun `minutes ago`() =
        assertEquals("5m ago", (baseNow - 5 * 60_000L).toRelativeTimeString(baseNow))

    @Test fun `hours ago`() =
        assertEquals("2h ago", (baseNow - 2 * 3_600_000L).toRelativeTimeString(baseNow))

    @Test fun `days ago`() =
        assertEquals("3d ago", (baseNow - 3 * 86_400_000L).toRelativeTimeString(baseNow))

    @Test fun `weeks ago`() =
        assertEquals("2w ago", (baseNow - 2 * 604_800_000L).toRelativeTimeString(baseNow))

    @Test fun `valid email passes`() = assertEquals(true, isValidEmail("alice@example.com"))
    @Test fun `invalid email fails`() = assertEquals(false, isValidEmail("not-an-email"))
    @Test fun `short name fails`() = assertEquals(false, isValidName("A"))
    @Test fun `valid name passes`() = assertEquals(true, isValidName("Alice"))
}

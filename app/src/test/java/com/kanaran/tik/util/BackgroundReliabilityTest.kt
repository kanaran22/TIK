package com.kanaran.tik.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which phones get the "keep reminders on time" card, and what it tells them. */
class BackgroundReliabilityTest {

    @Test
    fun `vivo and its iQOO sub-brand are recognised, whatever the casing`() {
        assertTrue(BackgroundReliability.isVivo("vivo", "vivo"))
        assertTrue(BackgroundReliability.isVivo("VIVO", "iQOO"))
        assertTrue(BackgroundReliability.isVivo("unknown", "iqoo"))
        assertTrue(BackgroundReliability.isVivo(" vivo ", null))
    }

    @Test
    fun `other brands are not mistaken for vivo`() {
        assertFalse(BackgroundReliability.isVivo("Xiaomi", "Redmi"))
        assertFalse(BackgroundReliability.isVivo(null, null))
    }

    @Test
    fun `battery-aggressive brands get the card`() {
        listOf("vivo" to "vivo", "Xiaomi" to "POCO", "OPPO" to "realme", "samsung" to "samsung", "OnePlus" to "OnePlus")
            .forEach { (m, b) -> assertTrue("$m/$b", BackgroundReliability.isAggressive(m, b)) }
    }

    @Test
    fun `stock-behaving phones do not get nagged`() {
        // Verified on stock Android: exact alarms fire on time even in deep Doze.
        assertFalse(BackgroundReliability.isAggressive("Google", "google"))
        assertFalse(BackgroundReliability.isAggressive("unknown", "generic"))
        assertFalse(BackgroundReliability.isAggressive(null, null))
    }

    @Test
    fun `vivo users get vivo's own settings named, including autostart and recents lock`() {
        val steps = BackgroundReliability.steps(vivo = true)
        assertEquals(4, steps.size)
        assertTrue(steps.any { "Autostart" in it })
        assertTrue(steps.any { "Background power consumption" in it })
        assertTrue(steps.any { "Recents" in it })
    }

    @Test
    fun `other brands get generic steps`() {
        val steps = BackgroundReliability.steps(vivo = false)
        assertTrue(steps.none { "i Manager" in it })
        assertTrue(steps.any { "Unrestricted" in it })
    }

    @Test
    fun `a forced brand overrides the real device`() {
        assertEquals("vivo" to "vivo", BackgroundReliability.device(forcedBrand = "vivo"))
    }
}

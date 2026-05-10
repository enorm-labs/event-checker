package de.norm.events.event

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyExtensionsTest {
    @Test
    fun `normalizeMoneyScale sets scale to 2 for whole numbers`() {
        assertEquals(BigDecimal("10.00"), BigDecimal("10").normalizeMoneyScale())
    }

    @Test
    fun `normalizeMoneyScale preserves scale 2 values unchanged`() {
        assertEquals(BigDecimal("10.00"), BigDecimal("10.00").normalizeMoneyScale())
    }

    @Test
    fun `normalizeMoneyScale truncates scale 1 to scale 2`() {
        assertEquals(BigDecimal("10.00"), BigDecimal("10.0").normalizeMoneyScale())
    }

    @Test
    fun `normalizeMoneyScale rounds scale 3 to scale 2 using HALF_UP`() {
        assertEquals(BigDecimal("10.13"), BigDecimal("10.125").normalizeMoneyScale())
        assertEquals(BigDecimal("10.12"), BigDecimal("10.124").normalizeMoneyScale())
    }

    @Test
    fun `normalizeMoneyScale handles zero`() {
        assertEquals(BigDecimal("0.00"), BigDecimal("0").normalizeMoneyScale())
        assertEquals(BigDecimal("0.00"), BigDecimal("0.0").normalizeMoneyScale())
    }

    @Test
    fun `normalized values are equal via BigDecimal equals`() {
        val fromScraper = BigDecimal("10").normalizeMoneyScale()
        val fromDatabase = BigDecimal("10.00").normalizeMoneyScale()
        assertEquals(fromScraper, fromDatabase)
    }
}

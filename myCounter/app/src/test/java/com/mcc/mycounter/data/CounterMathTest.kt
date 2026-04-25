package com.mcc.mycounter.data

import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.Periodicity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test puri (senza Android) sulla logica matematica del Counter.
 */
class CounterMathTest {

    private fun newCounter(
        currentValue: Long = 0,
        target: Long = 0,
        cost: Double = 0.0,
        reverse: Boolean = false,
        step: Int = 1
    ) = Counter(
        name = "test",
        step = step,
        startValue = 0,
        currentValue = currentValue,
        reverse = reverse,
        dailyTarget = target,
        costPerTap = cost,
        periodicity = Periodicity.DAILY.name
    )

    @Test
    fun totalCost_isCurrentValueTimesCostPerTap() {
        val c = newCounter(currentValue = 12, cost = 5.0)
        assertEquals(60.0, c.totalCost(), 0.001)
    }

    @Test
    fun isInHotZone_atOrAbove80PercentOfTarget() {
        val below = newCounter(currentValue = 7, target = 10)   // 70%
        val edge  = newCounter(currentValue = 8, target = 10)   // 80%
        val above = newCounter(currentValue = 12, target = 10)  // 120%
        assertFalse(below.isInHotZone())
        assertTrue(edge.isInHotZone())
        assertTrue(above.isInHotZone())
    }

    @Test
    fun isTargetReached_atOrAbove() {
        val below = newCounter(currentValue = 9, target = 10)
        val ok    = newCounter(currentValue = 10, target = 10)
        assertFalse(below.isTargetReached())
        assertTrue(ok.isTargetReached())
    }

    @Test
    fun targetReached_disabledWhenNoTarget() {
        val c = newCounter(currentValue = 100, target = 0)
        assertFalse(c.isTargetReached())
        assertFalse(c.isInHotZone())
    }

    @Test
    fun remainingToTarget_negativeWhenOver() {
        val c = newCounter(currentValue = 12, target = 10)
        assertEquals(-2L, c.remainingToTarget())
    }
}

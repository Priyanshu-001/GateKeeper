package org.example.core.strategies

import org.example.api.ResourceRules
import org.example.core.models.Identifier
import org.example.core.RateLimiterAlgorithm
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class FixedWindowRateLimiterAlgorithm(
    private val config: ResourceRules.AlgorithmConfig.FixedWindow.SingleInstance,
    private val clock: Clock = Clock.systemUTC()
) : RateLimiterAlgorithm {

    init {
        require(config.limit > 0) { "limit must be > 0" }
        require(config.windowDurationInSec > 0) { "windowDurationInSec must be > 0" }
    }

    private data class WindowState(
        var windowStartEpochMilli: Long,
        var consumedInWindow: Int
    )

    private val states = ConcurrentHashMap<String, WindowState>()
    private val windowSizeMillis = config.windowDurationInSec * 1000L

    override fun tryToConsume(identifier: Identifier): Boolean {
        val bucketKey = identifier.toString()
        val now = clock.millis()
        val state = states.computeIfAbsent(bucketKey) {
            WindowState(windowStartEpochMilli = now, consumedInWindow = 0)
        }

        synchronized(state) {
            advanceWindowIfNeeded(state, now)
            if (state.consumedInWindow >= config.limit) {
                return false
            }
            state.consumedInWindow += 1
            return true
        }
    }

    private fun advanceWindowIfNeeded(state: WindowState, nowEpochMilli: Long) {
        val elapsed = max(0L, nowEpochMilli - state.windowStartEpochMilli)
        if (elapsed < windowSizeMillis) {
            return
        }

        val windowsToSkip = elapsed / windowSizeMillis
        state.windowStartEpochMilli += windowsToSkip * windowSizeMillis
        state.consumedInWindow = 0
    }
}

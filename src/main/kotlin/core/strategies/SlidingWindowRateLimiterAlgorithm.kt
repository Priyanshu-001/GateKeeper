package org.example.core.strategies

import org.example.api.ResourceRules
import org.example.core.models.Identifier
import org.example.core.RateLimiterAlgorithm
import java.time.Clock
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

class SlidingWindowRateLimiterAlgorithm(
    private val config: ResourceRules.AlgorithmConfig.SlidingWindow.SingleInstance,
    private val clock: Clock = Clock.systemUTC()
) : RateLimiterAlgorithm {

    init {
        require(config.capacity > 0) { "capacity must be > 0" }
        require(config.refreshPerSec > 0) { "refreshPerSec must be > 0" }
    }

    private data class BucketState(val requestTimestamps: ArrayDeque<Long>)

    private val states = ConcurrentHashMap<String, BucketState>()
    private val windowMillis = (1000.0 / config.refreshPerSec * config.capacity).toLong().coerceAtLeast(1L)

    override fun tryToConsume(identifier: Identifier): Boolean {
        val bucketKey = identifier.toString()
        val now = clock.millis()
        val state = states.computeIfAbsent(bucketKey) {
            BucketState(requestTimestamps = ArrayDeque())
        }

        synchronized(state) {
            evictExpired(state, now)
            if (state.requestTimestamps.size >= config.capacity) {
                return false
            }
            state.requestTimestamps.addLast(now)
            return true
        }
    }

    private fun evictExpired(state: BucketState, nowEpochMilli: Long) {
        val cutoff = nowEpochMilli - windowMillis
        while (true) {
            val head = state.requestTimestamps.firstOrNull() ?: break
            if (head > cutoff) {
                break
            }
            state.requestTimestamps.removeFirst()
        }
    }
}

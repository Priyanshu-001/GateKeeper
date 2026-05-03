package org.example.core.strategies

import org.example.api.ResourceRules
import org.example.core.models.Identifier
import org.example.core.RateLimiterAlgorithm
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class TokenBucketRateLimiterAlgorithm(
    private val config: ResourceRules.AlgorithmConfig.TokenBucket.SingleInstance,
    private val clock: Clock = Clock.systemUTC()
) : RateLimiterAlgorithm {

    init {
        require(config.capacity > 0) { "capacity must be > 0" }
        require(config.refillPerSec > 0) { "refillPerSec must be > 0" }
    }

    private data class BucketState(
        var tokens: Double,
        var lastRefillEpochMilli: Long
    )

    private val buckets = ConcurrentHashMap<String, BucketState>()

    override fun tryToConsume(identifier: Identifier): Boolean {
        val bucketKey = identifier.toString()
        val now = clock.millis()
        val state = buckets.computeIfAbsent(bucketKey) {
            BucketState(tokens = config.capacity.toDouble(), lastRefillEpochMilli = now)
        }

        synchronized(state) {
            refill(state, now)
            if (state.tokens < 1.0) {
                return false
            }
            state.tokens -= 1.0
            return true
        }
    }

    private fun refill(state: BucketState, nowEpochMilli: Long) {
        val elapsedMillis = max(0L, nowEpochMilli - state.lastRefillEpochMilli)
        if (elapsedMillis <= 0L) {
            return
        }

        val tokensToAdd = (elapsedMillis / 1000.0) * config.refillPerSec
        state.tokens = minOf(config.capacity.toDouble(), state.tokens + tokensToAdd)
        state.lastRefillEpochMilli = nowEpochMilli
    }
}

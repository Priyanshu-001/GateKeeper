package org.example.core

import org.example.api.ResourceRules
import org.example.core.strategies.FixedWindowRateLimiterAlgorithm
import org.example.core.strategies.SlidingWindowRateLimiterAlgorithm
import org.example.core.strategies.TokenBucketRateLimiterAlgorithm
import java.time.Clock

object RateLimiterAlgorithmFactory {
    private val clock: Clock = Clock.systemUTC()

    fun create(config: ResourceRules.AlgorithmConfig): RateLimiterAlgorithm {
        return when (config) {
            is ResourceRules.AlgorithmConfig.SlidingWindow.SingleInstance ->
                SlidingWindowRateLimiterAlgorithm(config, clock)
            is ResourceRules.AlgorithmConfig.FixedWindow.SingleInstance ->
                FixedWindowRateLimiterAlgorithm(config, clock)
            is ResourceRules.AlgorithmConfig.TokenBucket.SingleInstance ->
                TokenBucketRateLimiterAlgorithm(config, clock)
        }
    }
}

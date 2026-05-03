package org.example.api

import java.util.Collections.emptyList

data class ResourceRules(
    val resource: String,
    val dimensions: List<Dimension> = emptyList(),
) {
    sealed class Dimension(open val algorithmConfig: AlgorithmConfig) {
        data class Keyed(val key: String, override val algorithmConfig: AlgorithmConfig) : Dimension(algorithmConfig)
        data class Global(override val algorithmConfig: AlgorithmConfig) : Dimension(algorithmConfig)
    }

    sealed class AlgorithmConfig {

        sealed class SlidingWindow(
            open val capacity: Int, open val refreshPerSec: Int
        ) : AlgorithmConfig() {

            data class SingleInstance(
                override val capacity: Int, override val refreshPerSec: Int
            ) : SlidingWindow(capacity, refreshPerSec)
        }

        sealed class FixedWindow(
            open val limit: Int, open val windowDurationInSec: Int
        ) : AlgorithmConfig() {

            data class SingleInstance(
                override val limit: Int, override val windowDurationInSec: Int
            ) : FixedWindow(limit, windowDurationInSec)
        }

        sealed class TokenBucket(
            open val capacity: Int, open val refillPerSec: Int
        ) : AlgorithmConfig() {

            data class SingleInstance(
                override val capacity: Int, override val refillPerSec: Int
            ) : TokenBucket(capacity, refillPerSec)
        }
    }
}

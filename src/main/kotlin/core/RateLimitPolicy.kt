package org.example.core

data class RateLimitPolicy(
    val dimensions: List<Dimension>
) {
    sealed class Dimension(open val algorithmConfig: AlgorithmConfig, mode: Mode) {
        data class Keyed(val key: String, override val algorithmConfig: AlgorithmConfig, val mode: Mode): Dimension(algorithmConfig, mode)
        data class Global( override val algorithmConfig: AlgorithmConfig, val mode: Mode): Dimension(algorithmConfig, mode)
    }

    enum class Mode {
        INSTANCE,
        DISTRIBUTED,
        DISTRIBUTED_BEST_EFFORT
    }
    sealed class  AlgorithmConfig{
        data class SlidingWindow(val capacity: Int, val refreshPerSec: Int): AlgorithmConfig()
        data class TokenBucket(val capacity: Int, val refillPerSec: Int): AlgorithmConfig()
        data class FixedWindow(val limit: Int, val windowDurationInSec: Int): AlgorithmConfig()
    }
}

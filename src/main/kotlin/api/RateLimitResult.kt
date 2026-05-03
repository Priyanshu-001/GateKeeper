package org.example.api

sealed class RateLimitResult {
    object Allowed: RateLimitResult()
    data class Denied(val violation: Violation): RateLimitResult() {
        sealed class Violation {
            abstract val resource: String
            abstract val key: String

            data class Global(
                override val resource: String
            ) : Violation() {
                override val key: String = "GLOBAL"
            }

            data class Dimension(
                override val resource: String,
                val dimension: String,
                override val key: String
            ) : Violation()
        }

    }
}

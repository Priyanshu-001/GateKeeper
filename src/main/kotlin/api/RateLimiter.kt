package org.example.api

abstract class RateLimiter(open val config: RateLimitContext) {

    abstract fun shouldAllow( resource: Resource): RateLimitResult
}


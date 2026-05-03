package org.example.core

import org.example.api.Identifier
import org.example.api.RateLimitContext
import org.example.api.RateLimitResult
import org.example.api.Resource
import org.example.api.ResourceRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RateLimiterImplTest {

    @Test
    fun allowsThenDeniesForSameRequestWithMultipleDimensions() {
        val limiter = RateLimiterImpl(
            RateLimitContext(
                rules = listOf(
                    ResourceRules(
                        resource = "checkout",
                        dimensions = listOf(
                            ResourceRules.Dimension.Keyed(
                                key = "userId",
                                algorithmConfig = ResourceRules.AlgorithmConfig.FixedWindow.SingleInstance(
                                    limit = 1,
                                    windowDurationInSec = 60
                                )
                            ),
                            ResourceRules.Dimension.Keyed(
                                key = "ip",
                                algorithmConfig = ResourceRules.AlgorithmConfig.FixedWindow.SingleInstance(
                                    limit = 2,
                                    windowDurationInSec = 60
                                )
                            )
                        )
                    )
                )
            )
        )

        val request = Resource(
            resource = "checkout",
            identifiers = listOf(
                Identifier.Dimension(dim = "userId", value = "u-1"),
                Identifier.Dimension(dim = "ip", value = "10.0.0.1")
            )
        )

        val first = limiter.shouldAllow(request)
        val second = limiter.shouldAllow(request)

        assertIs<RateLimitResult.Allowed>(first)
        val denied = assertIs<RateLimitResult.Denied>(second)
        val violation = assertIs<RateLimitResult.Denied.Violation.Dimension>(denied.violation)
        assertEquals("checkout", violation.resource)
        assertEquals("userId", violation.dimension)
        assertEquals("u-1", violation.key)
    }

    @Test
    fun allowsAgainAfterWindowResets() {
        val limiter = RateLimiterImpl(
            RateLimitContext(
                rules = listOf(
                    ResourceRules(
                        resource = "payments",
                        dimensions = listOf(
                            ResourceRules.Dimension.Keyed(
                                key = "userId",
                                algorithmConfig = ResourceRules.AlgorithmConfig.FixedWindow.SingleInstance(
                                    limit = 1,
                                    windowDurationInSec = 1
                                )
                            )
                        )
                    )
                )
            )
        )

        val request = Resource(
            resource = "payments",
            identifiers = listOf(
                Identifier.Dimension(dim = "userId", value = "u-42")
            )
        )

        val first = limiter.shouldAllow(request)
        val second = limiter.shouldAllow(request)
        Thread.sleep(1100)
        val third = limiter.shouldAllow(request)

        assertIs<RateLimitResult.Allowed>(first)
        assertIs<RateLimitResult.Denied>(second)
        assertIs<RateLimitResult.Allowed>(third)
    }
}

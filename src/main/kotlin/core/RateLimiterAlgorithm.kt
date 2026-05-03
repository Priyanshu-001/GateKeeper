package org.example.core

import org.example.core.models.Identifier

interface RateLimiterAlgorithm {
    fun tryToConsume(identifier: Identifier): Boolean
}

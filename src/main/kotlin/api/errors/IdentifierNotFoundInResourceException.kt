package org.example.api.errors

class IdentifierNotFoundInResourceException(
    val resource: String,
    val identifierDimension: String
) : RateLimiterException()

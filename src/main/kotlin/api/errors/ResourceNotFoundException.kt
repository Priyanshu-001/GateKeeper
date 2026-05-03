package org.example.api.errors

class ResourceNotFoundException(val resource: String): RateLimiterException() {
}
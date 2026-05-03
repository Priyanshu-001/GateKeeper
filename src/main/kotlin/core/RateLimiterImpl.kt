package org.example.core

import org.example.api.RateLimitContext
import org.example.api.ResourceRules
import org.example.api.RateLimitResult
import org.example.api.RateLimiter
import org.example.api.Resource
import org.example.api.errors.IdentifierNotFoundInResourceException
import org.example.api.errors.ResourceNotFoundException
import org.example.core.models.Identifier
import org.example.api.Identifier as ApiIdentifier
import java.util.concurrent.ConcurrentHashMap

class RateLimiterImpl(override val config: RateLimitContext): RateLimiter(config) {
    private var isReadToServe = false
    private val registry: MutableMap<String, ResourceConfig> = ConcurrentHashMap()
    private val factory = RateLimiterAlgorithmFactory
    private data class ResourceConfig(
        val keyed: MutableMap<ResourceRules.Dimension.Keyed, RateLimiterAlgorithm>?,
        val global: Pair<ResourceRules.Dimension.Global, RateLimiterAlgorithm>?
    )
    init {
        validate(config)
        registerConfigs(config)

    }

    override fun shouldAllow(resource: Resource): RateLimitResult {
        if(!isReadToServe) {
            throw RuntimeException("Not ready yet")
        }

        val resourceConfig = registry[resource.resource]
            ?:throw ResourceNotFoundException(resource.resource)

        resourceConfig.global?.let { (_, algorithm) ->
            if (!algorithm.tryToConsume(Identifier.Global(resource.resource))) {
                return RateLimitResult.Denied(
                    RateLimitResult.Denied.Violation.Global(resource.resource)
                )
            }
        }

        resource.identifiers.forEach { identifier ->
            if (identifier !is ApiIdentifier.Dimension) {
                return@forEach
            }
            val applicable = getApplicableRateLimiter(resourceConfig, identifier)
                ?: throw IdentifierNotFoundInResourceException(
                    resource = resource.resource,
                    identifierDimension = identifier.dim
                )
            val (_, algorithm) = applicable
            if (!algorithm.tryToConsume(Identifier.Dimension(resource.resource, identifier.dim, identifier.value))) {
                return RateLimitResult.Denied(
                    RateLimitResult.Denied.Violation.Dimension(
                        resource = resource.resource,
                        dimension = identifier.dim,
                        key = identifier.value
                    )
                )
            }
        }
        return RateLimitResult.Allowed
    }

    private fun validate(ctx: RateLimitContext) {
        val seenResources = mutableSetOf<String>()
        ctx.rules.forEach {
            validateAResource(it)
            require(seenResources.add(it.resource)) {
                "duplicate resource ${it.resource}"
            }
        }
    }

    private fun validateAResource(rules: ResourceRules) {
        val seenDimensions = mutableSetOf<String>()
        var globalCount = 0
        var keyedCount = 0

        for (dimension in rules.dimensions) {
            when (dimension) {

                is ResourceRules.Dimension.Global -> {
                    globalCount++

                    require(globalCount == 1) {
                        "Only one Global dimension allowed for resource=${rules.resource}"
                    }
                }

                is ResourceRules.Dimension.Keyed -> {
                    val added = seenDimensions.add(dimension.key)
                    keyedCount += 1
                    require(added) {
                        "Duplicate keyed dimension '${dimension.key}' for resource=${rules.resource}"
                    }
                }
            }
        }
        require(globalCount > 0 || keyedCount > 0) {
            "No actual rules provided"
        }
    }

    private fun registerConfigs(ctx: RateLimitContext) {
        ctx.rules.forEach { resourceRule ->
            val keyed = mutableMapOf<ResourceRules.Dimension.Keyed, RateLimiterAlgorithm>()
            var global: Pair<ResourceRules.Dimension.Global, RateLimiterAlgorithm>? = null

            resourceRule.dimensions.forEach { dimension ->
                when (dimension) {
                    is ResourceRules.Dimension.Keyed -> {
                        val algorithm = factory.create(dimension.algorithmConfig)
                        keyed[dimension] = algorithm
                    }
                    is ResourceRules.Dimension.Global -> {
                        val algorithm = factory.create(dimension.algorithmConfig)
                        global = Pair(dimension, algorithm)
                    }
                }
            }

            registry[resourceRule.resource] = ResourceConfig(
                keyed = keyed.ifEmpty { null },
                global = global
            )
        }

        isReadToServe = true
    }

    private fun getApplicableRateLimiter(
        resourceConfig: ResourceConfig,
        identifier: ApiIdentifier
    ): Pair<ResourceRules.Dimension.Keyed, RateLimiterAlgorithm>? {
        if (identifier !is ApiIdentifier.Dimension) {
            return null
        }
        val keyed = resourceConfig.keyed ?: return null
        return keyed.entries.firstOrNull { (dimension, _) ->
            dimension.key == identifier.dim
        }?.toPair()
    }
}

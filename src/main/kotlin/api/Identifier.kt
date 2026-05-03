package org.example.api

sealed class Identifier {
    data class Dimension(val dim: String, val value: String) : Identifier()
}

data class Resource(val identifiers: List<Identifier>, val resource: String)

package org.example.core.models

sealed class Identifier {
    abstract val resource: String

    class Dimension(
        override val resource: String,
        val dim: String,
        val value: String
    ) : Identifier()

    class Global(
        override val resource: String
    ) : Identifier()

    override fun toString(): String {
        return when (this) {
            is Dimension -> "$resource|$dim|$value"
            is Global -> "$resource|GLOBAL"
        }
    }
}
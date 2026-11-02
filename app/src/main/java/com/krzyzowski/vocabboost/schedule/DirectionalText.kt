package com.krzyzowski.vocabboost.schedule

import com.krzyzowski.vocabboost.session.CardDirection

internal object DirectionalText {
    fun encode(values: Map<CardDirection, String>): String =
        CardDirection.entries.joinToString("\n") { "${it.name}=${values.getValue(it)}" }

    fun decode(encoded: String): Map<CardDirection, String> {
        val result = mutableMapOf<CardDirection, String>()
        encoded.split('\n').forEach { line ->
            val parts = line.split('=', limit = 2)
            require(parts.size == 2) { "Invalid directional progress entry" }
            val direction = CardDirection.valueOf(parts[0])
            require(direction !in result) { "Duplicate progress direction" }
            result[direction] = parts[1]
        }
        require(result.keys == CardDirection.entries.toSet()) { "Missing progress direction" }
        return result
    }
}

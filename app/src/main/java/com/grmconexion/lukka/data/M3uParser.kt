package com.grmconexion.lukka.data

data class Channel(val name: String, val url: String, val group: String?)

/** Lector de listas M3U/M3U8 de canales (índice de canales, no un formato de video). */
object M3uParser {
    fun parse(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        var name: String? = null
        var group: String? = null
        for (raw in text.removePrefix("﻿").lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    name = line.substringAfterLast(',', "").trim().takeIf { it.isNotEmpty() }
                        ?: attr(line, "tvg-name")
                    group = attr(line, "group-title")
                }
                line.isEmpty() || line.startsWith("#") -> Unit
                else -> {
                    out += Channel(name ?: line, line, group)
                    name = null
                    group = null
                }
            }
        }
        return out
    }

    private fun attr(line: String, key: String): String? =
        Regex("""$key="([^"]*)"""", RegexOption.IGNORE_CASE)
            .find(line)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
}

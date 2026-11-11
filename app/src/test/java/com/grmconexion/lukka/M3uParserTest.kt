package com.grmconexion.lukka

import com.grmconexion.lukka.data.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uParserTest {
    @Test fun parsesNamesGroupsAndUrls() {
        val text = "﻿#EXTM3U\n" +
            "#EXTINF:-1 tvg-name=\"Uno\" group-title=\"Noticias\",Canal Uno\r\nhttp://ejemplo.invalid/uno.m3u8\r\n" +
            "#EXTINF:-1,Canal Dos\nhttp://ejemplo.invalid/dos.ts\n\n#COMENTARIO\n" +
            "http://ejemplo.invalid/sin-nombre\n"
        val list = M3uParser.parse(text)
        assertEquals(3, list.size)
        assertEquals("Canal Uno", list[0].name)
        assertEquals("Noticias", list[0].group)
        assertEquals("http://ejemplo.invalid/dos.ts", list[1].url)
        assertEquals("http://ejemplo.invalid/sin-nombre", list[2].name)
    }
}

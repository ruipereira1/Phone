package pt.familia.connect.facetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceTimeLinkTest {

    private val valid = "https://facetime.apple.com/join#v=1&p=AbCdEf123&k=XyZ987"

    @Test
    fun aceitaLinkOficial() {
        assertEquals(valid, FaceTimeLink.parse(valid)?.url)
    }

    @Test
    fun normalizaMaiusculasEEspacos() {
        assertEquals(valid, FaceTimeLink.parse("  HTTPS://FaceTime.Apple.com/join#v=1&p=AbCdEf123&k=XyZ987.  ")?.url)
    }

    @Test
    fun rejeitaOutrosDominiosEEsquemas() {
        assertNull(FaceTimeLink.parse("http://facetime.apple.com/join#v=1"))
        assertNull(FaceTimeLink.parse("https://facetime.apple.com.evil.example/join#v=1"))
        assertNull(FaceTimeLink.parse("https://evil.example/join#v=1&host=facetime.apple.com"))
        assertNull(FaceTimeLink.parse("https://user@facetime.apple.com/join#v=1"))
        assertNull(FaceTimeLink.parse("https://facetime.apple.com:8443/join#v=1"))
    }

    @Test
    fun rejeitaSemChaveOuCaminhoErrado() {
        assertNull(FaceTimeLink.parse("https://facetime.apple.com/join"))
        assertNull(FaceTimeLink.parse("https://facetime.apple.com/join#"))
        assertNull(FaceTimeLink.parse("https://facetime.apple.com/other#v=1"))
        assertNull(FaceTimeLink.parse(""))
        assertNull(FaceTimeLink.parse(null))
    }

    @Test
    fun encontraLinkNumaSms() {
        val sms = "Olá mãe! Liga-me aqui: $valid até já"
        assertEquals(valid, FaceTimeLink.findIn(sms)?.url)
        assertNull(FaceTimeLink.findIn("sem links https://example.com"))
    }

    @Test
    fun toStringNaoExpoeChave() {
        assertFalse(FaceTimeLink.parse(valid).toString().contains("XyZ987"))
    }

    @Test
    fun origemApple() {
        assertTrue(FaceTimeLink.isAppleOrigin("https://facetime.apple.com/join#x"))
        assertTrue(FaceTimeLink.isAppleOrigin("https://www.icloud.com/x"))
        assertFalse(FaceTimeLink.isAppleOrigin("https://apple.com.evil.example/"))
        assertFalse(FaceTimeLink.isAppleOrigin("javascript:alert(1)"))
        assertFalse(FaceTimeLink.isAppleOrigin(null))
    }
}

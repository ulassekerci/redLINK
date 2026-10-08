package org.metucet.redlink.protocol

import kotlin.random.Random
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamCodeTest {
    @Test
    fun `every team-code case passes`() {
        for (case in Vectors.teamCodes) {
            val name = case.string("name")
            val code = normaliseTeamCode(case.string("typed"))
            if (!case.getValue("valid").jsonPrimitive.boolean) {
                assertNull(name, code)
                continue
            }
            assertEquals(name, case.string("code"), code)
            assertEquals(name, case.string("lobby_id"), lobbyId(code!!))
            assertEquals(name, case.string("password"), hubPassword(code))
            assertEquals(name, case.string("viewer_id"), viewerId(code, case.string("token")))
        }
    }

    @Test
    fun `a generated code is 8 characters of the alphabet and passes its own check`() {
        repeat(100) {
            val code = generateTeamCode(Random)
            assertEquals(8, code.length)
            assertTrue(code, code.all { it in ALPHABET })
            assertEquals(code, normaliseTeamCode(code))
        }
    }

    // The check characters are worked by hand: seven of the first entry sum to
    // 0, and seven of the last to 30 x 28 = 840, which is 3 modulo 31.
    @Test
    fun `a generated code takes its first seven characters from the random source`() {
        assertEquals("22222222", generateTeamCode(Always(0)))
        assertEquals("ZZZZZZZ5", generateTeamCode(Always(30)))
    }

    @Test
    fun `a code is shown as XXXX-XXXX`() {
        assertEquals("K7QM-3XPC", formatTeamCode("K7QM3XPC"))
    }
}

// A random source that always picks the same alphabet entry.
private class Always(private val index: Int) : Random() {
    override fun nextBits(bitCount: Int) = throw UnsupportedOperationException()
    override fun nextInt(until: Int) = index
}

package org.metucet.redlink.protocol

import kotlin.random.Random

// The team code and the hub identities built from it, as the hub usage spec's
// "Team code and hub identities" says. A code here is the 8 characters without
// the dash, as normaliseTeamCode returns it.

const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"

private const val CODE_LENGTH = 8
private const val TOKEN_LENGTH = 8
private const val LOBBY_PREFIX = "REDLINK"

private fun inAlphabet(text: String) = text.all { it in ALPHABET }

// The first seven positions in the alphabet, weighted 1 to 7, summed modulo 31.
private fun checkCharacter(first7: String): Char {
    val sum = first7.withIndex().sumOf { (i, char) -> ALPHABET.indexOf(char) * (i + 1) }
    return ALPHABET[sum % ALPHABET.length]
}

// Reads a typed code: lower case is accepted, dashes and spaces are ignored.
// Returns the code, or null when it is not a valid one.
fun normaliseTeamCode(typed: String): String? {
    val code = typed.uppercase().filterNot { it == '-' || it.isWhitespace() }
    if (code.length != CODE_LENGTH || !inAlphabet(code)) return null
    if (checkCharacter(code.take(CODE_LENGTH - 1)) != code.last()) return null
    return code
}

// A new code: seven random characters and their check character. The caller
// picks the random source.
fun generateTeamCode(random: Random): String {
    val first7 = String(CharArray(CODE_LENGTH - 1) { ALPHABET[random.nextInt(ALPHABET.length)] })
    return first7 + checkCharacter(first7)
}

// A code as it is shown: XXXX-XXXX.
fun formatTeamCode(code: String) = "${code.take(4)}-${code.drop(4)}"

fun lobbyId(code: String) = LOBBY_PREFIX + code

fun viewerId(code: String, token: String) = lobbyId(code) + token

fun hubPassword(code: String) = code

fun isToken(text: String) = text.length == TOKEN_LENGTH && inAlphabet(text)

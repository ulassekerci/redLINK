// The team code and the hub identities built from it, as the hub usage spec's
// "Team code and hub identities" says. A code here is the 8 characters without
// the dash, as normaliseTeamCode returns it.

export const ALPHABET = '23456789ABCDEFGHJKMNPQRSTUVWXYZ'

const CODE_LENGTH = 8
const TOKEN_LENGTH = 8
const LOBBY_PREFIX = 'REDLINK'

const inAlphabet = (text: string) => [...text].every((char) => ALPHABET.includes(char))

// The first seven positions in the alphabet, weighted 1 to 7, summed modulo 31.
function checkCharacter(first7: string) {
  const sum = [...first7].reduce((total, char, i) => total + ALPHABET.indexOf(char) * (i + 1), 0)
  return ALPHABET[sum % ALPHABET.length]
}

// Reads a typed code: lower case is accepted, dashes and spaces are ignored.
// Returns the code, or null when it is not a valid one.
export function normaliseTeamCode(typed: string) {
  const code = typed.toUpperCase().replace(/[-\s]/g, '')
  if (code.length !== CODE_LENGTH || !inAlphabet(code)) return null
  if (checkCharacter(code.slice(0, 7)) !== code[7]) return null
  return code
}

export const lobbyId = (code: string) => LOBBY_PREFIX + code

export const viewerId = (code: string, token: string) => lobbyId(code) + token

export const hubPassword = (code: string) => code

export function isToken(text: string) {
  return text.length === TOKEN_LENGTH && inAlphabet(text)
}

// random returns a number in [0, 1), as Math.random does.
export function generateToken(random: () => number) {
  return Array.from({ length: TOKEN_LENGTH }, () => ALPHABET[Math.floor(random() * ALPHABET.length)]).join('')
}

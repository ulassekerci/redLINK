import { describe, expect, test } from 'vitest'
import vectors from '../../../protocol/vectors.json'
import { formatTeamCode, generateToken, hubPassword, isToken, lobbyId, normaliseTeamCode, viewerId } from '.'

type TeamCodeCase =
  | { name: string; typed: string; valid: false }
  | { name: string; typed: string; valid: true; code: string; lobby_id: string; password: string; token: string; viewer_id: string }

const cases = vectors.team_codes as TeamCodeCase[]

describe('team-code', () => {
  test.each(cases.filter((c) => !c.valid))('$name is rejected', ({ typed }) => {
    expect(normaliseTeamCode(typed)).toBeNull()
  })

  test.each(cases.filter((c) => c.valid))('$name', (c) => {
    const code = normaliseTeamCode(c.typed)
    expect(code).toBe(c.code)
    expect(lobbyId(c.code)).toBe(c.lobby_id)
    expect(hubPassword(c.code)).toBe(c.password)
    expect(viewerId(c.code, c.token)).toBe(c.viewer_id)
  })

  test('a code is shown as XXXX-XXXX', () => {
    expect(formatTeamCode('K7QM3XPC')).toBe('K7QM-3XPC')
  })

  test('a generated token is 8 characters of the alphabet', () => {
    for (const random of [() => 0, () => 0.999999, Math.random]) {
      const token = generateToken(random)
      expect(token).toHaveLength(8)
      expect(isToken(token)).toBe(true)
    }
    expect(generateToken(() => 0)).toBe('22222222')
    expect(generateToken(() => 0.999999)).toBe('ZZZZZZZZ')
  })
})

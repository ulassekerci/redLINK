import { describe, expect, test } from 'vitest'
import vectors from '../../../protocol/vectors.json'
import { FrameDecoder, decodeMessage, encodeMessage, type DecodedMessage, type EncodableMessage } from '.'

type DeframeCase = { name: string; check: 'deframe'; reads: string[]; payloads: string[] }
type EncodeCase = { name: string; check: 'encode'; frame: string; message: EncodableMessage }
type DecodeCase = { name: string; check: 'decode'; frame: string; message: DecodedMessage | null }
type BothCase = { name: string; check: 'both'; frame: string; message: EncodableMessage & DecodedMessage }
type FrameCase = DeframeCase | EncodeCase | DecodeCase | BothCase

const cases = vectors.frames as FrameCase[]

const fromHex = (hex: string) => Uint8Array.from(hex.match(/../g) ?? [], (byte) => parseInt(byte, 16))
const toHex = (bytes: Uint8Array) => Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')

describe('protocol-vectors', () => {
  test('every case has a known check', () => {
    const checks = new Set(cases.map((c) => c.check))
    expect([...checks].sort()).toEqual(['both', 'decode', 'deframe', 'encode'])
  })

  test.each(cases.filter((c) => c.check === 'deframe'))('$name', ({ reads, payloads }) => {
    const decoder = new FrameDecoder()
    const decoded = reads.flatMap((read) => decoder.push(fromHex(read)))
    expect(decoded.map(toHex)).toEqual(payloads)
  })

  test.each(cases.filter((c) => c.check === 'encode' || c.check === 'both'))('$name encodes', ({ message, frame }) => {
    expect(toHex(encodeMessage(message as EncodableMessage))).toBe(frame)
  })

  test.each(cases.filter((c) => c.check === 'decode' || c.check === 'both'))('$name decodes', ({ frame, message }) => {
    const payloads = new FrameDecoder().push(fromHex(frame))
    expect(payloads).toHaveLength(1)
    expect(decodeMessage(payloads[0])).toEqual(message)
  })
})

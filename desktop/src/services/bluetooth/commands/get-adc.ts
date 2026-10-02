import type { Payload } from '../payload'

export function parseGetDecodedADC(payload: Payload) {
  const response = {
    level1: 0,
    voltage1: 0,
    level2: 0,
    voltage2: 0,
  }

  response.level1 = payload.readDouble32(1e6)
  response.voltage1 = payload.readDouble32(1e6)
  response.level2 = payload.readDouble32(1e6)
  response.voltage2 = payload.readDouble32(1e6)

  return response
}

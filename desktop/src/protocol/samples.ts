import type { DecodedAdc, Gps, ValuesSetup } from './messages'

// What main hands the renderer, and what the direct link writes to the store:
// a parsed message stamped with its arrival time (the laptop's clock, Unix
// milliseconds). Both paths make them here so they agree on the shape.

// Position, board ID, number of boards and battery capacity are parsed but
// not handed on. A field missing from a short reply stays absent.
export type BoardSample = Omit<ValuesSetup, 'type' | 'position' | 'board_id' | 'board_count' | 'battery_capacity_wh'> & {
  received_at: number
}

export type AdcSample = Omit<DecodedAdc, 'type'> & { received_at: number }

export type GpsFix = Omit<Gps, 'type'> & { received_at: number }

export function boardSample(message: ValuesSetup, receivedAt: number): BoardSample {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { type, position, board_id, board_count, battery_capacity_wh, ...fields } = message
  return { ...fields, received_at: receivedAt }
}

export function adcSample(message: DecodedAdc, receivedAt: number): AdcSample {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { type, ...fields } = message
  return { ...fields, received_at: receivedAt }
}

export function gpsFix(message: Gps, receivedAt: number): GpsFix {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { type, ...fields } = message
  return { ...fields, received_at: receivedAt }
}

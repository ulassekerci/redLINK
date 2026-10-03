import { describe, expect, test } from 'vitest'
import vectors from '../../../../../protocol/vectors.json'
import { encodeMessage } from '../../../protocol'
import { startPolling, type Clock, type PollingEvents, type Transport, type TransportListener } from './polling'

const frameOf = (name: string) => {
  const hex = vectors.frames.find((c) => c.name === name)!.frame as string
  return Uint8Array.from(hex.match(/../g)!, (byte) => parseInt(byte, 16))
}

const SETUP_REPLY = frameOf('reply-values-setup')
const ADC_REPLY = frameOf('reply-decoded-adc')
const SETUP_REQUEST = encodeMessage({ type: 'values_setup_request' })
const ADC_REQUEST = encodeMessage({ type: 'decoded_adc_request' })

const flush = async () => {
  for (let i = 0; i < 10; i++) await Promise.resolve()
}

class FakeClock implements Clock {
  private time = 1_000_000
  private timers = new Map<number, { at: number; fn: () => void }>()
  private nextId = 1

  now = () => this.time

  setTimeout = (fn: () => void, ms: number) => {
    const id = this.nextId++
    this.timers.set(id, { at: this.time + ms, fn })
    return id
  }

  clearTimeout = (id: unknown) => {
    this.timers.delete(id as number)
  }

  // Moves time forward, running each timer when its time comes and letting
  // promises settle in between.
  async advance(ms: number) {
    const end = this.time + ms
    await flush()
    for (;;) {
      const due = [...this.timers].filter(([, t]) => t.at <= end).sort(([a, x], [b, y]) => x.at - y.at || a - b)[0]
      if (!due) break
      const [id, timer] = due
      this.timers.delete(id)
      this.time = timer.at
      timer.fn()
      await flush()
    }
    this.time = end
  }
}

class FakeTransport implements Transport {
  writes: { at: number; bytes: Uint8Array }[] = []
  connectAttempts: number[] = []
  closed = false
  // Whether the next connect succeeds.
  reachable = true
  private listener: TransportListener | null = null

  constructor(private clock: FakeClock) {}

  async connect(listener: TransportListener) {
    this.connectAttempts.push(this.clock.now())
    if (!this.reachable) throw new Error('unreachable')
    this.listener = listener
  }

  async write(bytes: Uint8Array) {
    this.writes.push({ at: this.clock.now(), bytes })
  }

  close() {
    this.closed = true
    this.listener = null
  }

  receive(bytes: Uint8Array) {
    this.listener?.bytes(bytes)
  }

  drop() {
    const listener = this.listener
    this.listener = null
    listener?.dropped()
  }

  // The requests written so far, by name.
  requests() {
    return this.writes.map(({ bytes }) => name(bytes))
  }
}

const name = (bytes: Uint8Array) => {
  const hex = String(bytes)
  if (hex === String(SETUP_REQUEST)) return 'setup'
  if (hex === String(ADC_REQUEST)) return 'adc'
  return hex
}

const setup = () => {
  const clock = new FakeClock()
  const transport = new FakeTransport(clock)
  const events = { samples: [] as unknown[], states: [] as string[] }
  const listener: PollingEvents = {
    boardSample: (sample) => events.samples.push(sample),
    adcSample: (sample) => events.samples.push(sample),
    state: (state) => events.states.push(state),
  }
  const polling = startPolling(transport, clock, listener)
  return { clock, transport, events, polling }
}

describe('direct-link', () => {
  test('asks for the setup values, then the decoded ADC once the setup reply is in', async () => {
    const { clock, transport } = setup()
    await clock.advance(0)
    expect(transport.requests()).toEqual(['setup'])

    transport.receive(SETUP_REPLY)
    await clock.advance(0)
    expect(transport.requests()).toEqual(['setup', 'adc'])
  })

  test('starts a cycle every 50 ms when the board answers at once', async () => {
    const { clock, transport } = setup()
    await clock.advance(0)
    const start = clock.now()
    for (let cycle = 0; cycle < 3; cycle++) {
      await clock.advance(10)
      transport.receive(SETUP_REPLY)
      await clock.advance(10)
      transport.receive(ADC_REPLY)
      await clock.advance(30)
    }

    expect(transport.writes.map(({ at, bytes }) => [at - start, name(bytes)])).toEqual([
      [0, 'setup'],
      [10, 'adc'],
      [50, 'setup'],
      [60, 'adc'],
      [100, 'setup'],
      [110, 'adc'],
      [150, 'setup'],
    ])
  })

  test('gives up on a command after 250 ms and sends the next', async () => {
    const { clock, transport } = setup()
    await clock.advance(0)
    const start = clock.now()
    await clock.advance(600)

    expect(transport.writes.map(({ at, bytes }) => [at - start, name(bytes)])).toEqual([
      [0, 'setup'],
      [250, 'adc'],
      [500, 'setup'],
    ])
  })

  test('hands on each reply as a sample stamped with its arrival time', async () => {
    const { clock, transport, events } = setup()
    await clock.advance(7)
    transport.receive(SETUP_REPLY)
    await clock.advance(5)
    transport.receive(ADC_REPLY)

    expect(events.samples).toEqual([
      expect.objectContaining({ speed_m_s: 8.333, battery_voltage_v: 48.1, received_at: 1_000_007 }),
      { adc_level1: 0.512345, adc_voltage1: 1.691, adc_level2: -0.25, adc_voltage2: 0, received_at: 1_000_012 },
    ])
    expect(events.samples[0]).not.toHaveProperty('position')
    expect(events.samples[0]).not.toHaveProperty('board_id')
  })

  test('leaves out the fields a short setup reply does not reach', async () => {
    const { clock, transport, events } = setup()
    await clock.advance(0)
    transport.receive(frameOf('reply-values-setup-short'))

    expect(events.samples[0]).toHaveProperty('fault_code', 0)
    expect(events.samples[0]).not.toHaveProperty('odometer_m')
    expect(events.samples[0]).not.toHaveProperty('board_uptime_ms')
  })

  test('puts together a reply that arrives in pieces', async () => {
    const { clock, transport, events } = setup()
    await clock.advance(0)
    transport.receive(SETUP_REPLY.slice(0, 20))
    transport.receive(SETUP_REPLY.slice(20, 50))
    expect(events.samples).toEqual([])
    transport.receive(SETUP_REPLY.slice(50))
    await clock.advance(0)

    expect(events.samples).toEqual([expect.objectContaining({ speed_m_s: 8.333 })])
    expect(transport.requests()).toEqual(['setup', 'adc'])
  })

  test('the board is answering from its first reply', async () => {
    const { clock, transport, events } = setup()
    await clock.advance(0)
    expect(events.states).toEqual([])

    transport.receive(SETUP_REPLY)
    expect(events.states).toEqual(['answering'])
  })

  test('the board is not answering after 2 s of silence', async () => {
    const { clock, transport, events } = setup()
    await clock.advance(0)
    transport.receive(SETUP_REPLY)
    await clock.advance(1999)
    expect(events.states).toEqual(['answering'])

    await clock.advance(1)
    expect(events.states).toEqual(['answering', 'not_answering'])
  })

  test('a silent board gets a fresh link every 2 s, on the same transport', async () => {
    const { clock, transport } = setup()
    await clock.advance(0)
    const start = clock.now()
    transport.receive(SETUP_REPLY)
    await clock.advance(6500)

    expect(transport.closed).toBe(true)
    expect(transport.connectAttempts.map((at) => at - start)).toEqual([0, 2000, 4000, 6000])
    // Each fresh link polls again.
    expect(transport.writes.filter(({ at }) => at - start === 4000).map(({ bytes }) => name(bytes))).toEqual(['setup'])
  })

  test('a board that never answers is not answering 2 s after the link is up', async () => {
    const { clock, events } = setup()
    await clock.advance(1999)
    expect(events.states).toEqual([])

    await clock.advance(1)
    expect(events.states).toEqual(['not_answering'])
  })

  test('the board answering again after silence resumes the samples', async () => {
    const { clock, transport, events } = setup()
    await clock.advance(2500)
    transport.receive(SETUP_REPLY)

    expect(events.states).toEqual(['not_answering', 'answering'])
    expect(events.samples).toHaveLength(1)
  })

  test('the board is not answering at once when the link drops, and polling stops', async () => {
    const { clock, transport, events } = setup()
    await clock.advance(0)
    transport.receive(SETUP_REPLY)
    await clock.advance(100)
    transport.drop()
    expect(events.states).toEqual(['answering', 'not_answering'])

    const writes = transport.writes.length
    await clock.advance(1999)
    expect(transport.writes).toHaveLength(writes)
  })

  test('after a drop it tries to reconnect every 2 s until it gets the link back', async () => {
    const { clock, transport } = setup()
    await clock.advance(0)
    transport.reachable = false
    transport.drop()
    const dropped = clock.now()
    await clock.advance(6500)

    expect(transport.connectAttempts.slice(1).map((at) => at - dropped)).toEqual([2000, 4000, 6000])

    transport.reachable = true
    await clock.advance(2000)
    expect(transport.connectAttempts.slice(1).map((at) => at - dropped)).toEqual([2000, 4000, 6000, 8000])
    transport.receive(SETUP_REPLY)
    await clock.advance(1000)
    transport.receive(SETUP_REPLY)
    await clock.advance(1000)
    expect(transport.connectAttempts).toHaveLength(5)
  })

  test('a link that cannot be made at first is retried every 2 s', async () => {
    const clock = new FakeClock()
    const transport = new FakeTransport(clock)
    transport.reachable = false
    const states: string[] = []
    startPolling(transport, clock, { boardSample: () => {}, adcSample: () => {}, state: (s) => states.push(s) })
    await clock.advance(4500)

    expect(states).toEqual(['not_answering'])
    expect(transport.connectAttempts.map((at) => at - transport.connectAttempts[0])).toEqual([0, 2000, 4000])
  })

  test('samples resume when the link is back and the board answers', async () => {
    const { clock, transport, events } = setup()
    await clock.advance(0)
    transport.receive(SETUP_REPLY.slice(0, 30))
    transport.drop()
    await clock.advance(2000)
    expect(transport.requests().at(-1)).toBe('setup')

    // The half frame from before the drop is not glued to the new reply.
    transport.receive(SETUP_REPLY)
    expect(events.states).toEqual(['not_answering', 'answering'])
    expect(events.samples).toEqual([expect.objectContaining({ speed_m_s: 8.333 })])
  })

  test('stopping closes the link and stops polling and reconnecting', async () => {
    const { clock, transport, polling } = setup()
    await clock.advance(0)
    polling.stop()
    expect(transport.closed).toBe(true)

    const writes = transport.writes.length
    await clock.advance(10_000)
    expect(transport.writes).toHaveLength(writes)
    expect(transport.connectAttempts).toHaveLength(1)
  })

  test('stopping while the link is being made closes it once it is up', async () => {
    const clock = new FakeClock()
    const transport = new FakeTransport(clock)
    const polling = startPolling(transport, clock, { boardSample: () => {}, adcSample: () => {}, state: () => {} })
    polling.stop()
    transport.closed = false
    await clock.advance(1000)

    expect(transport.closed).toBe(true)
    expect(transport.writes).toEqual([])
  })

  test('never asks for the firmware version', async () => {
    const { clock, transport } = setup()
    await clock.advance(0)
    for (let cycle = 0; cycle < 5; cycle++) {
      transport.receive(SETUP_REPLY)
      transport.receive(ADC_REPLY)
      await clock.advance(50)
    }
    await clock.advance(3000)

    expect(new Set(transport.requests())).toEqual(new Set(['setup', 'adc']))
  })
})

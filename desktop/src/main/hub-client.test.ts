import { describe, expect, test } from 'vitest'
import vectors from '../../../protocol/vectors.json'
import type { ConnectionState, StreamSample } from '../preload/api'
import { ALPHABET } from '../protocol'
import { startHubClient, type Clock, type HubSocket, type HubSocketEvents } from './hub-client'

const bytesOf = (hex: string) => Uint8Array.from(hex.match(/../g)!, (byte) => parseInt(byte, 16))
const frameOf = (name: string) => bytesOf(vectors.frames.find((c) => c.name === name)!.frame as string)
const ascii = (text: string) => new TextEncoder().encode(text)

const HOST = 'hub.example.org'
const PORT = 65101
const START = 1_000_000

class FakeClock implements Clock {
  private time = START
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

  // Moves time forward, running each timer when its time comes.
  advance(ms: number) {
    const end = this.time + ms
    for (;;) {
      const due = [...this.timers].filter(([, t]) => t.at <= end).sort(([a, x], [b, y]) => x.at - y.at || a - b)[0]
      if (!due) break
      const [id, timer] = due
      this.timers.delete(id)
      this.time = timer.at
      timer.fn()
    }
    this.time = end
  }
}

// One connection the client opened, with what it wrote and when. The test
// plays the hub: it connects it, sends it bytes and closes it.
class FakeSocket implements HubSocket {
  openedAt: number
  writes: { at: number; bytes: Uint8Array }[] = []
  // Closed by the client.
  closed = false

  constructor(
    private clock: FakeClock,
    private events: HubSocketEvents,
  ) {
    this.openedAt = clock.now()
  }

  write(bytes: Uint8Array) {
    this.writes.push({ at: this.clock.now(), bytes })
  }

  close() {
    this.closed = true
  }

  connect() {
    this.events.connected()
  }

  receive(bytes: Uint8Array) {
    this.events.data(bytes)
  }

  // The hub closed it, or it could not be made.
  drop() {
    this.events.closed()
  }

  // The first write as text: the login line.
  get login() {
    return new TextDecoder().decode(this.writes[0]?.bytes)
  }

  // Connects it and answers its PING.
  answer(reply: 'PONG' | 'NULL') {
    this.connect()
    this.receive(ascii(`${reply}\n`))
  }
}

// The fixed random source: the queued numbers first, then always `value`.
class FixedRandom {
  value = 0.5
  private queue: number[] = []

  next = () => this.queue.shift() ?? this.value

  // Queues the numbers that make the next generated token this one.
  token(token: string) {
    this.queue.push(...Array.from(token, (char) => (ALPHABET.indexOf(char) + 0.5) / ALPHABET.length))
  }
}

const LOBBY_PING = 'PING:REDLINKK7QM3XPC:0\n'
const LOBBY_LOGIN = 'VESCTOOL:REDLINKK7QM3XPC:K7QM3XPC\n'
// The token and viewer ID of the code-valid vector.
const TOKEN = '4HT9WQ2B'
const VIEWER_PING = 'PING:REDLINKK7QM3XPC4HT9WQ2B:0\n'
const VIEWER_LOGIN = 'VESCTOOL:REDLINKK7QM3XPC4HT9WQ2B:K7QM3XPC\n'
const LOBBY_REQUEST = frameOf('lobby-request')
const HEARTBEAT = frameOf('heartbeat')
// Status from a phone on major version 1.
const ANSWERING = frameOf('status-answering')
const UNREACHABLE = frameOf('status-unreachable')
// Status from a phone on major version 2, board answering.
const ANSWERING_V2 = frameOf('status-trailing-bytes')

const messageOf = (name: string) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { type, ...fields } = vectors.frames.find((c) => c.name === name)!.message as Record<string, unknown>
  return fields
}

const text = (bytes: Uint8Array) => new TextDecoder().decode(bytes)

const setup = (teamCode: string | null = 'K7QM3XPC', majorVersion = 1) => {
  const clock = new FakeClock()
  const sockets: FakeSocket[] = []
  const destinations: string[] = []
  const random = new FixedRandom()
  const states: ConnectionState[] = []
  const samples: StreamSample[] = []
  const client = startHubClient(
    { teamCode, hubHost: HOST, hubPort: PORT, majorVersion },
    {
      connect: (host, port, events) => {
        destinations.push(`${host}:${port}`)
        const socket = new FakeSocket(clock, events)
        sockets.push(socket)
        return socket
      },
      clock,
      random: random.next,
    },
    { state: (state) => states.push(state), sample: (sample) => samples.push(sample) },
  )
  const last = () => sockets[sockets.length - 1]

  // Takes the visit from its pending lobby PING to the end of the lobby
  // request, with the given token. The newest socket is then the first PING of
  // the viewer's own ID.
  const requestRegistration = (token = TOKEN) => {
    random.token(token)
    last().answer('PONG')
    const lobby = last()
    lobby.connect()
    clock.advance(500)
    return lobby
  }

  // Takes the visit from its pending lobby PING to the viewer attached to a
  // registration of its own, which is returned.
  const attach = (token = TOKEN) => {
    requestRegistration(token)
    last().answer('PONG')
    const viewer = last()
    viewer.connect()
    return viewer
  }

  return { clock, sockets, destinations, random, states, samples, client, last, requestRegistration, attach }
}

describe('hub-client', () => {
  test('with no team code it does nothing', () => {
    const { clock, sockets, states, client } = setup(null)
    clock.advance(600_000)
    expect(sockets).toEqual([])
    expect(states).toEqual([])
    expect(client.state()).toEqual({ state: 'no_team_code' })
  })

  describe('a lobby visit', () => {
    test('starts on launch with a PING of the lobby ID, on the stored host and port', () => {
      const { sockets, destinations } = setup()
      expect(destinations).toEqual([`${HOST}:${PORT}`])
      sockets[0].connect()
      expect(sockets[0].writes).toHaveLength(1)
      expect(sockets[0].login).toBe('PING:REDLINKK7QM3XPC:0\n')
    })

    test('is hub unreachable when the connection cannot be made', () => {
      const { sockets, client } = setup()
      sockets[0].drop()
      expect(client.state()).toEqual({ state: 'hub_unreachable' })
    })

    test('is hub unreachable when the hub does not answer within 5 s', () => {
      const { clock, sockets, client } = setup()
      clock.advance(4999)
      expect(client.state()).toEqual({ state: 'joining' })
      clock.advance(1)
      expect(client.state()).toEqual({ state: 'hub_unreachable' })
      expect(sockets[0].closed).toBe(true)
    })

    test('is phone not found on NULL', () => {
      const { sockets, client } = setup()
      sockets[0].answer('NULL')
      expect(client.state()).toEqual({ state: 'phone_not_found' })
    })

    test('reads an answer that arrives in pieces', () => {
      const { sockets, client } = setup()
      sockets[0].connect()
      sockets[0].receive(ascii('NU'))
      expect(client.state()).toEqual({ state: 'joining' })
      sockets[0].receive(ascii('LL\n'))
      expect(client.state()).toEqual({ state: 'phone_not_found' })
    })

    test('is joining on PONG, and attaches to the lobby with the login line as its own write', () => {
      const { clock, sockets, client, states, last } = setup()
      sockets[0].answer('NULL')
      clock.advance(2000)
      last().answer('PONG')
      expect(states).toEqual([{ state: 'phone_not_found' }, { state: 'joining' }])
      expect(client.state()).toEqual({ state: 'joining' })

      const lobby = last()
      lobby.connect()
      expect(lobby.writes).toHaveLength(1)
      expect(lobby.login).toBe(LOBBY_LOGIN)
    })

    test('writes the lobby request every 100 ms for 500 ms, each as its own write, then closes', () => {
      const { clock, random, sockets, last } = setup()
      random.token(TOKEN)
      sockets[0].answer('PONG')
      const lobby = last()
      lobby.connect()
      const connectedAt = clock.now()

      clock.advance(499)
      expect(lobby.closed).toBe(false)
      clock.advance(10_000)
      const requests = lobby.writes.slice(1)
      expect(requests.map((write) => write.at - connectedAt)).toEqual([100, 200, 300, 400, 500])
      for (const request of requests) expect(request.bytes).toEqual(LOBBY_REQUEST)
      expect(lobby.closed).toBe(true)
    })

    test('then PINGs its own ID every 250 ms for 2 s', () => {
      const { clock, sockets, client, requestRegistration } = setup()
      requestRegistration()
      const from = clock.now()
      const before = sockets.length - 1

      for (let ms = 0; ms < 2000; ms += 250) {
        const ping = sockets[sockets.length - 1]
        ping.connect()
        expect(ping.login).toBe(VIEWER_PING)
        ping.receive(ascii('NULL\n'))
        clock.advance(250)
      }
      const pings = sockets.slice(before, before + 8)
      expect(pings.map((ping) => ping.openedAt - from)).toEqual([0, 250, 500, 750, 1000, 1250, 1500, 1750])
      // The window is over: what comes next is a new visit, not a ninth PING.
      expect(sockets).toHaveLength(before + 8)
      expect(client.state()).toEqual({ state: 'joining' })
    })

    test('attaches to its own registration on PONG and stops asking', () => {
      const { clock, sockets, client, last, requestRegistration } = setup()
      requestRegistration()
      last().answer('NULL')
      clock.advance(250)
      last().answer('PONG')

      const viewer = last()
      viewer.connect()
      expect(viewer.login).toBe(VIEWER_LOGIN)
      const opened = sockets.length
      clock.advance(2000)
      expect(sockets).toHaveLength(opened)
      expect(client.state()).toEqual({ state: 'joining' })
    })

    test('goes on to PING its own ID when another viewer displaces it in the lobby', () => {
      const { clock, random, sockets, last } = setup()
      random.token(TOKEN)
      sockets[0].answer('PONG')
      const lobby = last()
      lobby.connect()
      clock.advance(150)
      lobby.drop()
      last().connect()
      expect(last().login).toBe(VIEWER_PING)
      clock.advance(10_000)
      expect(lobby.writes).toHaveLength(2)
    })

    test('gives the visit up when the lobby connection is not made within 5 s', () => {
      const { clock, sockets, last } = setup()
      sockets[0].answer('PONG')
      const lobby = last()
      clock.advance(5000)
      expect(lobby.closed).toBe(true)
      expect(lobby.writes).toEqual([])
      clock.advance(1500)
      last().connect()
      expect(last().login).toBe(LOBBY_PING)
    })

    test('ignores a PONG that arrives after the 2 s', () => {
      const { clock, sockets, last, requestRegistration } = setup()
      requestRegistration()
      const late = last()
      clock.advance(2000)
      const opened = sockets.length
      late.answer('PONG')
      expect(sockets).toHaveLength(opened)
    })
  })

  describe('between visits', () => {
    test('waits a random 1 to 2 s', () => {
      for (const [value, wait] of [
        [0, 1000],
        [0.5, 1500],
        [0.999, 1999],
      ]) {
        const { clock, random, sockets } = setup()
        random.value = value
        sockets[0].answer('NULL')
        clock.advance(wait - 1)
        expect(sockets).toHaveLength(1)
        clock.advance(1)
        expect(sockets).toHaveLength(2)
        sockets[1].connect()
        expect(sockets[1].login).toBe(LOBBY_PING)
      }
    })

    test('waits after hub unreachable too', () => {
      const { clock, sockets } = setup()
      sockets[0].drop()
      clock.advance(1500)
      expect(sockets).toHaveLength(2)
    })

    test('waits when its own ID never answers PONG, then visits again with a fresh token', () => {
      const { clock, sockets, last, random, requestRegistration } = setup()
      requestRegistration()
      clock.advance(2000)
      const opened = sockets.length
      clock.advance(1499)
      expect(sockets).toHaveLength(opened)
      clock.advance(1)
      last().connect()
      expect(last().login).toBe(LOBBY_PING)

      random.token('ZZZZZZZZ')
      last().answer('PONG')
      last().connect()
      clock.advance(500)
      last().connect()
      expect(last().login).toBe('PING:REDLINKK7QM3XPCZZZZZZZZ:0\n')
    })

    test('waits 5 s after a minute with no PONG from the lobby, and 1 to 2 s again after one', () => {
      const { clock, sockets, last } = setup()
      const visits: number[] = []
      for (let answered = 0; clock.now() - START <= 75_000; clock.advance(500)) {
        for (; answered < sockets.length; answered++) {
          visits.push(sockets[answered].openedAt - START)
          sockets[answered].answer('NULL')
        }
      }
      expect(visits.slice(0, 3)).toEqual([0, 1500, 3000])
      expect(visits.slice(-5)).toEqual([58_500, 60_000, 65_000, 70_000, 75_000])

      // The next visit, at 80 s, finds the lobby but gets no registration.
      clock.advance(4500)
      expect(last().openedAt - START).toBe(80_000)
      last().answer('PONG')
      last().connect()
      clock.advance(500 + 2000)
      const opened = sockets.length
      clock.advance(1500)
      expect(sockets).toHaveLength(opened + 1)
      expect(last().openedAt - START).toBe(84_000)
    })
  })

  describe('attached', () => {
    test('sends a heartbeat once a second, each as its own write', () => {
      const { clock, attach } = setup()
      const viewer = attach()
      const attachedAt = clock.now()
      expect(viewer.writes).toHaveLength(1)

      for (let second = 0; second < 5; second++) {
        viewer.receive(ANSWERING)
        clock.advance(1000)
      }
      const heartbeats = viewer.writes.slice(1)
      expect(heartbeats.map((write) => write.at - attachedAt)).toEqual([100, 1100, 2100, 3100, 4100])
      for (const heartbeat of heartbeats) expect(heartbeat.bytes).toEqual(HEARTBEAT)
    })

    test('is live when status says the board is answering', () => {
      const { client, attach } = setup()
      attach().receive(ANSWERING)
      expect(client.state()).toEqual({ state: 'live' })
    })

    test('is board unreachable when status says so, and live again when it answers', () => {
      const { states, attach } = setup()
      const viewer = attach()
      viewer.receive(UNREACHABLE)
      viewer.receive(UNREACHABLE)
      viewer.receive(ANSWERING)
      viewer.receive(UNREACHABLE)
      expect(states).toEqual([{ state: 'board_unreachable' }, { state: 'live' }, { state: 'board_unreachable' }])
    })

    test('is version mismatch, with both versions, when the phone is on a newer major', () => {
      const { client, attach } = setup('K7QM3XPC', 1)
      attach().receive(ANSWERING_V2)
      expect(client.state()).toEqual({ state: 'version_mismatch', phoneMajor: 2, appMajor: 1 })
    })

    test('is version mismatch when the phone is on an older major', () => {
      const { client, attach } = setup('K7QM3XPC', 2)
      attach().receive(UNREACHABLE)
      expect(client.state()).toEqual({ state: 'version_mismatch', phoneMajor: 1, appMajor: 2 })
    })

    test('in version mismatch stays attached, keeps its heartbeat and hands on no samples', () => {
      const { clock, sockets, samples, client, attach } = setup('K7QM3XPC', 1)
      const viewer = attach()
      const opened = sockets.length
      for (let second = 0; second < 10; second++) {
        viewer.receive(ANSWERING_V2)
        viewer.receive(frameOf('reply-values-setup'))
        viewer.receive(frameOf('reply-decoded-adc'))
        viewer.receive(frameOf('gps'))
        clock.advance(1000)
      }
      expect(client.state()).toEqual({ state: 'version_mismatch', phoneMajor: 2, appMajor: 1 })
      expect(viewer.closed).toBe(false)
      expect(sockets).toHaveLength(opened)
      expect(viewer.writes.slice(1).map((write) => write.bytes)).toEqual(Array(10).fill(HEARTBEAT))
      expect(samples).toEqual([])
    })

    test('is phone lost after 3 s without status, and visits the lobby with a fresh token', () => {
      const { clock, sockets, random, client, last, attach } = setup()
      const viewer = attach()
      viewer.receive(ANSWERING)
      clock.advance(2000)
      viewer.receive(ANSWERING)
      clock.advance(2999)
      expect(client.state()).toEqual({ state: 'live' })
      const opened = sockets.length

      clock.advance(1)
      expect(client.state()).toEqual({ state: 'phone_lost' })
      expect(viewer.closed).toBe(true)
      // The lobby, at once, and never the old ID.
      expect(sockets).toHaveLength(opened + 1)
      last().connect()
      expect(last().login).toBe(LOBBY_PING)

      random.token('ZZZZZZZZ')
      last().answer('PONG')
      expect(client.state()).toEqual({ state: 'joining' })
      last().connect()
      clock.advance(500)
      last().answer('PONG')
      last().connect()
      expect(last().login).toBe('VESCTOOL:REDLINKK7QM3XPCZZZZZZZZ:K7QM3XPC\n')

      const logins = sockets.slice(opened).map((socket) => socket.login)
      expect(logins).not.toContain(VIEWER_PING)
      expect(logins).not.toContain(VIEWER_LOGIN)
    })

    test('leaves version mismatch only through phone lost', () => {
      const { clock, states, attach } = setup('K7QM3XPC', 1)
      const viewer = attach()
      viewer.receive(ANSWERING_V2)
      viewer.receive(ANSWERING)
      clock.advance(3000)
      expect(states.slice(-2)).toEqual([
        { state: 'version_mismatch', phoneMajor: 2, appMajor: 1 },
        { state: 'phone_lost' },
      ])
    })

    test('after phone lost is phone not found, hub unreachable or joining, as the lobby answers', () => {
      const { clock, states, last, attach } = setup()
      attach().receive(ANSWERING)
      clock.advance(3000)
      last().answer('NULL')
      clock.advance(1500)
      last().drop()
      clock.advance(1500)
      last().answer('PONG')
      expect(states.slice(-4)).toEqual([
        { state: 'phone_lost' },
        { state: 'phone_not_found' },
        { state: 'hub_unreachable' },
        { state: 'joining' },
      ])
    })

    test('is phone lost when no status ever arrives', () => {
      const { clock, client, attach } = setup()
      const viewer = attach()
      clock.advance(2999)
      expect(client.state()).toEqual({ state: 'joining' })
      clock.advance(1)
      expect(client.state()).toEqual({ state: 'phone_lost' })
      expect(viewer.closed).toBe(true)
    })

    test('stops its heartbeat once the phone is lost', () => {
      const { clock, attach } = setup()
      const viewer = attach()
      clock.advance(3000)
      const written = viewer.writes.length
      clock.advance(10_000)
      expect(viewer.writes).toHaveLength(written)
    })

    test('visits the lobby at once when the hub closes its socket', () => {
      const { sockets, client, last, attach } = setup()
      const viewer = attach()
      viewer.receive(ANSWERING)
      const opened = sockets.length
      viewer.drop()
      expect(client.state()).toEqual({ state: 'phone_lost' })
      expect(sockets).toHaveLength(opened + 1)
      last().connect()
      expect(last().login).toBe(LOBBY_PING)
    })

    test('after a long time live, looks for a lost phone at the 1 to 2 s pace', () => {
      const { clock, sockets, last, attach } = setup()
      const viewer = attach()
      for (let second = 0; second < 120; second++) {
        viewer.receive(ANSWERING)
        clock.advance(1000)
      }
      viewer.drop()
      last().answer('NULL')
      const opened = sockets.length
      clock.advance(1500)
      expect(sockets).toHaveLength(opened + 1)
    })

    test('writes nothing to the hub but login lines, lobby requests and heartbeats', () => {
      const { clock, sockets, attach } = setup()
      const viewer = attach()
      for (let second = 0; second < 5; second++) {
        viewer.receive(ANSWERING)
        viewer.receive(frameOf('reply-values-setup'))
        viewer.receive(frameOf('gps'))
        clock.advance(1000)
      }
      clock.advance(10_000)

      const allowed = [LOBBY_PING, LOBBY_LOGIN, VIEWER_PING, VIEWER_LOGIN, text(LOBBY_REQUEST), text(HEARTBEAT)]
      const written = sockets.flatMap((socket) => socket.writes.map((write) => text(write.bytes)))
      expect(written.filter((bytes) => !allowed.includes(bytes))).toEqual([])
      expect(new Set(written)).toEqual(new Set(allowed))
    })
  })

  describe('the stream', () => {
    test('hands on board samples, ADC samples and GPS fixes stamped with their arrival time', () => {
      const { clock, samples, attach } = setup()
      const viewer = attach()
      viewer.receive(ANSWERING)

      clock.advance(10)
      const boardAt = clock.now()
      viewer.receive(frameOf('reply-values-setup'))
      clock.advance(10)
      const adcAt = clock.now()
      viewer.receive(frameOf('reply-decoded-adc'))
      clock.advance(10)
      const gpsAt = clock.now()
      viewer.receive(frameOf('gps'))

      // Position, board ID, number of boards and battery capacity are not
      // handed on.
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { position, board_id, board_count, battery_capacity_wh, ...board } = messageOf('reply-values-setup')
      expect(samples).toEqual([
        { board: { ...board, received_at: boardAt } },
        { adc: { ...messageOf('reply-decoded-adc'), received_at: adcAt } },
        { gps: { ...messageOf('gps'), received_at: gpsAt } },
      ])
    })

    test('leaves a field missing from a short setup reply absent', () => {
      const { samples, attach } = setup()
      const viewer = attach()
      viewer.receive(ANSWERING)
      viewer.receive(frameOf('reply-values-setup-short'))
      const [sample] = samples as { board: object }[]
      expect(sample.board).toHaveProperty('distance_abs_m', 1300)
      expect(sample.board).not.toHaveProperty('odometer_m')
      expect(sample.board).not.toHaveProperty('board_uptime_ms')
    })

    test('reads frames split across reads and several frames in one read', () => {
      const { samples, client, attach } = setup()
      const viewer = attach()
      const adc = frameOf('reply-decoded-adc')
      viewer.receive(Uint8Array.from([...ANSWERING, ...adc.slice(0, 7)]))
      expect(client.state()).toEqual({ state: 'live' })
      expect(samples).toEqual([])
      viewer.receive(Uint8Array.from([...adc.slice(7), ...frameOf('gps')]))
      expect(samples.map((sample) => Object.keys(sample))).toEqual([['adc'], ['gps']])
    })

    test('hands on nothing before the first status', () => {
      const { samples, attach } = setup()
      const viewer = attach()
      viewer.receive(frameOf('reply-values-setup'))
      viewer.receive(frameOf('gps'))
      expect(samples).toEqual([])
    })

    test('keeps handing on GPS fixes while the board is unreachable', () => {
      const { samples, attach } = setup()
      const viewer = attach()
      viewer.receive(UNREACHABLE)
      viewer.receive(frameOf('gps'))
      expect(samples.map((sample) => Object.keys(sample))).toEqual([['gps']])
    })

    test('ignores unknown command IDs, unknown message types and a firmware version reply', () => {
      const { states, samples, attach } = setup()
      const viewer = attach()
      viewer.receive(ANSWERING)
      viewer.receive(frameOf('unknown-command'))
      viewer.receive(frameOf('custom-unknown-type'))
      viewer.receive(frameOf('reply-fw-version'))
      viewer.receive(frameOf('heartbeat'))
      viewer.receive(frameOf('lobby-request'))
      expect(samples).toEqual([])
      expect(states[states.length - 1]).toEqual({ state: 'live' })

      viewer.receive(frameOf('reply-decoded-adc'))
      expect(samples).toHaveLength(1)
    })

    test('ignores trailing bytes after a known message', () => {
      const { samples, client, clock, attach } = setup('K7QM3XPC', 2)
      const viewer = attach()
      viewer.receive(ANSWERING_V2)
      expect(client.state()).toEqual({ state: 'live' })
      viewer.receive(frameOf('gps-trailing-bytes'))
      expect(samples).toEqual([{ gps: { ...messageOf('gps-trailing-bytes'), received_at: clock.now() } }])
    })

    test('does not let an unknown message stand in for status', () => {
      const { clock, client, attach } = setup()
      const viewer = attach()
      viewer.receive(ANSWERING)
      for (let ms = 0; ms < 3000; ms += 500) {
        clock.advance(500)
        viewer.receive(frameOf('reply-values-setup'))
        viewer.receive(frameOf('custom-unknown-type'))
      }
      expect(client.state()).toEqual({ state: 'phone_lost' })
    })
  })
})

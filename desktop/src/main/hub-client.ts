import type { ConnectionState, Settings, StreamSample } from '../preload/api'
import {
  FrameDecoder,
  adcSample,
  boardSample,
  decodeMessage,
  encodeMessage,
  generateToken,
  gpsFix,
  hubPassword,
  lobbyId,
  viewerId,
  type DecodedMessage,
} from '../protocol'

// The viewer's side of the hub usage spec, "Joining, viewer side": it visits
// the lobby for a registration of its own, attaches, sends its heartbeat and
// hands on the stream. It talks to a socket factory, a clock and a random
// source it is given, so tests can drive it.

// One TCP connection to the hub.
export interface HubSocket {
  write: (bytes: Uint8Array) => void
  close: () => void
}

export interface HubSocketEvents {
  connected: () => void
  data: (bytes: Uint8Array) => void
  // The hub closed the connection, or it could not be made. Not called after
  // the client's own close.
  closed: () => void
}

export type HubSocketFactory = (host: string, port: number, events: HubSocketEvents) => HubSocket

export interface Clock {
  now: () => number
  setTimeout: (fn: () => void, ms: number) => unknown
  clearTimeout: (handle: unknown) => void
}

export interface HubClientSettings extends Settings {
  // The app's own major version, compared with the one in the status message.
  majorVersion: number
}

export interface HubClientSeams {
  connect: HubSocketFactory
  clock: Clock
  // A number in [0, 1), as Math.random returns.
  random: () => number
}

export interface HubClientEvents {
  // Called on each change.
  state: (state: ConnectionState) => void
  sample: (sample: StreamSample) => void
}

type PingReply = 'PONG' | 'NULL' | 'unreachable'

// How long the hub has to answer before it counts as unreachable.
const ANSWER_TIMEOUT_MS = 5000
// A write in or right after the login line's is lost on the hub, so the first
// one follows it by this long.
const AFTER_LOGIN_MS = 100
const LOBBY_REQUEST_MS = 100
const LOBBY_REQUESTS = 5
const OWN_PING_MS = 250
const OWN_PING_WINDOW_MS = 2000
// After this long with no PONG from the lobby, visits are SLOW_WAIT_MS apart.
const SLOW_AFTER_MS = 60_000
const SLOW_WAIT_MS = 5000
const HEARTBEAT_MS = 1000
const STATUS_TIMEOUT_MS = 3000

const HEARTBEAT = encodeMessage({ type: 'heartbeat' })

const ascii = new TextEncoder()

const sameState = (a: ConnectionState, b: ConnectionState) =>
  a.state === b.state &&
  (a.state !== 'version_mismatch' ||
    b.state !== 'version_mismatch' ||
    (a.phoneMajor === b.phoneMajor && a.appMajor === b.appMajor))

// Joins the hub and keeps trying for as long as the app is open. With no team
// code it does nothing, and while a direct link is in use it is off the hub.
export function startHubClient(settings: HubClientSettings, seams: HubClientSeams, events: HubClientEvents) {
  const { majorVersion } = settings
  const { connect, clock, random } = seams
  let { teamCode, hubHost, hubPort } = settings
  let onDirectLink = false
  const launchState = (): ConnectionState => ({ state: teamCode ? 'joining' : 'no_team_code' })
  let state = launchState()

  // The client is in one step at a time. A step's sockets and timers end with
  // it, and a socket of an earlier step is no longer listened to.
  let step = 0
  const sockets = new Set<HubSocket>()
  const timers = new Set<unknown>()

  const nextStep = () => {
    step += 1
    for (const timer of timers) clock.clearTimeout(timer)
    for (const socket of sockets) socket.close()
    timers.clear()
    sockets.clear()
  }

  const after = (ms: number, fn: () => void) => {
    const timer = clock.setTimeout(() => {
      timers.delete(timer)
      fn()
    }, ms)
    timers.add(timer)
    return timer
  }

  const cancel = (timer: unknown) => {
    clock.clearTimeout(timer)
    timers.delete(timer)
  }

  const open = (socketEvents: HubSocketEvents) => {
    const opened = step
    const current = () => opened === step
    const socket = connect(hubHost, hubPort, {
      connected: () => current() && socketEvents.connected(),
      data: (bytes) => current() && socketEvents.data(bytes),
      closed: () => current() && socketEvents.closed(),
    })
    sockets.add(socket)
    return socket
  }

  const setState = (next: ConnectionState) => {
    if (sameState(state, next)) return
    state = next
    events.state(next)
  }

  // Asks the hub whether an ID is registered, on a connection of its own.
  const ping = (id: string, answered: (reply: PingReply) => void) => {
    let text = ''
    let done = false
    const finish = (reply: PingReply) => {
      if (done) return
      done = true
      socket.close()
      answered(reply)
    }
    const socket = open({
      connected: () => socket.write(ascii.encode(`PING:${id}:0\n`)),
      data: (bytes) => {
        text += new TextDecoder().decode(bytes)
        if (!text.includes('\n')) return
        const line = text.slice(0, text.indexOf('\n'))
        finish(line === 'PONG' || line === 'NULL' ? line : 'unreachable')
      },
      closed: () => finish('unreachable'),
    })
  }

  const start = (code: string) => {
    const lobby = lobbyId(code)
    const password = hubPassword(code)
    const login = (id: string) => ascii.encode(`VESCTOOL:${id}:${password}\n`)
    // When the lobby last answered PONG, or when the client began to look.
    let lobbySeenAt = clock.now()

    // Step 1: is the bridge holding the lobby?
    const visitLobby = () => {
      nextStep()
      const answered = (reply: PingReply) => {
        if (reply !== 'PONG') {
          setState({ state: reply === 'NULL' ? 'phone_not_found' : 'hub_unreachable' })
          return waitThenVisit()
        }
        lobbySeenAt = clock.now()
        setState({ state: 'joining' })
        requestRegistration()
      }
      ping(lobby, answered)
      after(ANSWER_TIMEOUT_MS, () => answered('unreachable'))
    }

    // Steps 2 to 4: ask the bridge, through the lobby, for a registration
    // under a fresh token. The repeats ride out the windows in which the hub
    // discards bytes; the visit is short because the lobby holds one viewer.
    const requestRegistration = () => {
      nextStep()
      const token = generateToken(random)
      const request = encodeMessage({ type: 'lobby_request', token })
      let written = 0
      const write = () => {
        socket.write(request)
        written += 1
        if (written < LOBBY_REQUESTS) after(LOBBY_REQUEST_MS, write)
        else findRegistration(token)
      }
      const socket = open({
        connected: () => {
          socket.write(login(lobby))
          after(AFTER_LOGIN_MS, write)
        },
        data: () => {},
        // Another viewer's visit displaced this one. A request may have got
        // through before it did.
        closed: () => findRegistration(token),
      })
      after(ANSWER_TIMEOUT_MS, waitThenVisit)
    }

    // Steps 5 and 6: has the bridge opened the registration?
    const findRegistration = (token: string) => {
      nextStep()
      const id = viewerId(code, token)
      const ask = () =>
        ping(id, (reply) => {
          if (reply === 'PONG') attach(id)
        })
      ask()
      for (let ms = OWN_PING_MS; ms < OWN_PING_WINDOW_MS; ms += OWN_PING_MS) after(ms, ask)
      after(OWN_PING_WINDOW_MS, waitThenVisit)
    }

    // Attached: the heartbeat keeps the registration active, and status says
    // the bridge is alive and whether the board answers it.
    const attach = (id: string) => {
      nextStep()
      const decoder = new FrameDecoder()
      let statusTimer: unknown

      // Back to the lobby for a new registration. The old ID is not asked
      // after: the hub may answer PONG for it long after the bridge is gone.
      const lost = () => {
        setState({ state: 'phone_lost' })
        lobbySeenAt = clock.now()
        visitLobby()
      }
      const awaitStatus = () => {
        cancel(statusTimer)
        statusTimer = after(STATUS_TIMEOUT_MS, lost)
      }
      const heartbeat = () => {
        socket.write(HEARTBEAT)
        after(HEARTBEAT_MS, heartbeat)
      }

      const receive = (message: DecodedMessage | null, receivedAt: number) => {
        if (message?.type === 'status') {
          awaitStatus()
          // Version mismatch is left only through phone lost.
          if (state.state === 'version_mismatch') return
          if (message.protocol_version !== majorVersion) {
            setState({ state: 'version_mismatch', phoneMajor: message.protocol_version, appMajor: majorVersion })
          } else {
            setState({ state: message.board_state === 0 ? 'live' : 'board_unreachable' })
          }
          return
        }
        // Nothing is handed on before status has shown the phone to be on
        // this app's major version.
        if (state.state !== 'live' && state.state !== 'board_unreachable') return
        if (message?.type === 'values_setup') events.sample({ board: boardSample(message, receivedAt) })
        else if (message?.type === 'decoded_adc') events.sample({ adc: adcSample(message, receivedAt) })
        else if (message?.type === 'gps') events.sample({ gps: gpsFix(message, receivedAt) })
      }

      const socket = open({
        connected: () => {
          socket.write(login(id))
          after(AFTER_LOGIN_MS, heartbeat)
        },
        data: (bytes) => {
          const receivedAt = clock.now()
          for (const payload of decoder.push(bytes)) receive(decodeMessage(payload), receivedAt)
        },
        closed: lost,
      })
      awaitStatus()
    }

    // A random 1 to 2 s, so viewers returning together do not displace each
    // other round after round.
    const waitThenVisit = () => {
      nextStep()
      const slow = clock.now() - lobbySeenAt >= SLOW_AFTER_MS
      after(slow ? SLOW_WAIT_MS : 1000 + random() * 1000, visitLobby)
    }

    visitLobby()
  }

  // Closes the sockets, which ends the heartbeat, so the bridge closes the
  // registration by itself. The state is then what it is on launch.
  const leave = () => {
    nextStep()
    setState(launchState())
  }

  // Joins as on launch, with the settings as they now are, having left what
  // it was on. Not while a direct link is in use.
  const join = () => {
    leave()
    if (teamCode && !onDirectLink) start(teamCode)
  }

  join()

  return {
    // The state now, for a renderer that starts after it was last sent.
    state: () => state,
    // A board was picked for a direct link: the client leaves the hub and
    // visits the lobby no more until the direct link has ended.
    directLinkStarted: () => {
      if (onDirectLink) return
      onDirectLink = true
      leave()
    },
    directLinkEnded: () => {
      if (!onDirectLink) return
      onDirectLink = false
      join()
    },
    // A different team code, host or port makes the client leave and join
    // again with the new values. Saving what was stored leaves it alone.
    settingsSaved: (saved: Settings) => {
      if (saved.teamCode === teamCode && saved.hubHost === hubHost && saved.hubPort === hubPort) return
      teamCode = saved.teamCode
      hubHost = saved.hubHost
      hubPort = saved.hubPort
      join()
    },
  }
}

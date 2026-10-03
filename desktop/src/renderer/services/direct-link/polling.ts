import {
  FrameDecoder,
  adcSample,
  boardSample,
  decodeMessage,
  encodeMessage,
  type AdcSample,
  type BoardSample,
} from '../../../protocol'

// The direct link's polling loop: the bridge's cycle from the hub spec's
// "Polling and the stream", run against the board from the renderer. It
// talks to a transport and a clock it is given, so tests can drive it.

export interface TransportListener {
  bytes: (bytes: Uint8Array) => void
  // The link went down by itself.
  dropped: () => void
}

// A link to the board: write bytes, receive bytes, connected or not.
export interface Transport {
  // Resolves once the link is up; rejects when it could not be made.
  connect: (listener: TransportListener) => Promise<void>
  write: (bytes: Uint8Array) => Promise<void>
  close: () => void
}

export interface Clock {
  now: () => number
  setTimeout: (fn: () => void, ms: number) => unknown
  clearTimeout: (handle: unknown) => void
}

export type BoardState = 'answering' | 'not_answering'

export interface PollingEvents {
  boardSample: (sample: BoardSample) => void
  adcSample: (sample: AdcSample) => void
  // Called on each change. Until the first, the link is being made.
  state: (state: BoardState) => void
}

type Request = 'values_setup' | 'decoded_adc'

const CYCLE_MS = 50
const REPLY_TIMEOUT_MS = 250
const SILENCE_MS = 2000
const RECONNECT_MS = 2000

const requests: Record<Request, Uint8Array> = {
  values_setup: encodeMessage({ type: 'values_setup_request' }),
  decoded_adc: encodeMessage({ type: 'decoded_adc_request' }),
}

// Connects the transport and polls the board until stopped. A link that
// cannot be made, drops or goes silent is made again every 2 s, on the same
// transport.
export function startPolling(transport: Transport, clock: Clock, events: PollingEvents) {
  let stopped = false
  let linkUp = false
  let state: BoardState | null = null
  let decoder = new FrameDecoder()
  let waitingFor: Request | null = null
  let cycleStart = 0
  let replyTimer: unknown
  let cycleTimer: unknown
  let silenceTimer: unknown
  let reconnectTimer: unknown

  const setState = (next: BoardState) => {
    if (state === next) return
    state = next
    events.state(next)
  }

  // The board is not answering after 2 s with no reply at all.
  const heard = () => {
    clock.clearTimeout(silenceTimer)
    silenceTimer = clock.setTimeout(silent, SILENCE_MS)
  }

  // Each request waits for its reply for at most 250 ms.
  const send = (request: Request) => {
    waitingFor = request
    replyTimer = clock.setTimeout(() => moveOn(request), REPLY_TIMEOUT_MS)
    // A failed write is a missing reply; a lost link reports itself.
    transport.write(requests[request]).catch(() => {})
  }

  // Moves the cycle on from a request, answered or given up on.
  const moveOn = (request: Request) => {
    if (waitingFor !== request) return
    clock.clearTimeout(replyTimer)
    if (request === 'values_setup') {
      send('decoded_adc')
    } else {
      // The next cycle starts 50 ms after this one did, or at once if this
      // one ran over.
      waitingFor = null
      cycleTimer = clock.setTimeout(startCycle, Math.max(0, cycleStart + CYCLE_MS - clock.now()))
    }
  }

  const startCycle = () => {
    cycleStart = clock.now()
    send('values_setup')
  }

  const receive = (bytes: Uint8Array) => {
    for (const payload of decoder.push(bytes)) {
      const message = decodeMessage(payload)
      if (message?.type === 'values_setup') events.boardSample(boardSample(message, clock.now()))
      else if (message?.type === 'decoded_adc') events.adcSample(adcSample(message, clock.now()))
      else continue
      heard()
      setState('answering')
      moveOn(message.type)
    }
  }

  const stopTimers = () => {
    for (const timer of [replyTimer, cycleTimer, silenceTimer, reconnectTimer]) clock.clearTimeout(timer)
    waitingFor = null
  }

  const lost = () => {
    linkUp = false
    stopTimers()
    setState('not_answering')
    reconnectTimer = clock.setTimeout(connect, RECONNECT_MS)
  }

  // A link with 2 s of silence on it is replaced at once: the 2 s have passed.
  const silent = () => {
    linkUp = false
    stopTimers()
    setState('not_answering')
    transport.close()
    connect()
  }

  const connect = () => {
    const listener: TransportListener = {
      bytes: (bytes) => {
        if (!stopped && linkUp) receive(bytes)
      },
      dropped: () => {
        if (!stopped && linkUp) lost()
      },
    }
    transport.connect(listener).then(
      () => {
        if (stopped) return transport.close()
        linkUp = true
        // Half a frame from before a drop must not be glued to the next one.
        decoder = new FrameDecoder()
        heard()
        startCycle()
      },
      () => {
        if (!stopped) lost()
      },
    )
  }

  connect()

  return {
    stop: () => {
      stopped = true
      linkUp = false
      stopTimers()
      transport.close()
    },
  }
}

// The wire format of protocol/README.md: framing, the board commands, our
// messages and the team code, with the public hub's address. Plain TypeScript with no Electron, Node or DOM
// imports, so main uses it for the hub and the renderer for the direct link.
export { crc16 } from './crc'
export { FrameDecoder, encodeFrame } from './framing'
export { PUBLIC_HUB } from './hub'
export {
  COMM_CUSTOM_APP_DATA,
  COMM_FW_VERSION,
  COMM_GET_DECODED_ADC,
  COMM_GET_VALUES_SETUP,
  decodeMessage,
  encodeMessage,
  type DecodedAdc,
  type DecodedMessage,
  type EncodableMessage,
  type FwVersion,
  type Gps,
  type Heartbeat,
  type LobbyRequest,
  type Status,
  type ValuesSetup,
} from './messages'
export { adcSample, boardSample, gpsFix, type AdcSample, type BoardSample, type GpsFix } from './samples'
export {
  ALPHABET,
  formatTeamCode,
  generateToken,
  hubPassword,
  isToken,
  lobbyId,
  normaliseTeamCode,
  viewerId,
} from './team-code'

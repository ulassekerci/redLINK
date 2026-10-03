import { create } from 'zustand'
import type { ConnectionState, Settings, TypedSettings } from '../../preload/api'
import { onDirectLink, useDirectLinkStore } from './directLink'
import { useVehicleStore } from './vehicle'

// This laptop as a viewer: what main's hub client reports.
interface HubState {
  // Joining until main has answered, which it does at once.
  connection: ConnectionState
  // The stream is on the dashboard: live or board unreachable, and phone lost
  // when it follows one of those.
  watching: boolean
  // The settings the hub client runs on: those read on launch or last saved.
  // Null until main has answered.
  settings: Settings | null
}

export const useHubStore = create<HubState>()(() => ({
  connection: { state: 'joining' },
  watching: false,
  settings: null,
}))

const showsStream = ({ state }: ConnectionState) => state === 'live' || state === 'board_unreachable'

// Main leaves the hub when a board is picked for a direct link, but what it
// sent just before may arrive after. That is kept out of the vehicle store.
const fillsStore = () => !onDirectLink(useDirectLinkStore.getState().phase)

const setConnection = (connection: ConnectionState) => {
  useHubStore.setState(({ watching }) => ({
    connection,
    watching: showsStream(connection) || (connection.state === 'phone_lost' && watching),
  }))
  // In version mismatch nothing from the stream is shown, old values included.
  if (connection.state === 'version_mismatch' && fillsStore()) {
    useVehicleStore.setState({ board: null, adc: null, gps: null })
  }
}

// The state is read once on start, for what main sent before the renderer was
// listening. A change that arrives first is newer than that answer.
let heard = false
window.redlink.onConnectionState((connection) => {
  heard = true
  setConnection(connection)
})
window.redlink.readConnectionState().then((connection) => {
  if (!heard) setConnection(connection)
})
window.redlink.readSettings().then((settings) => useHubStore.setState({ settings }))

// Stores the settings screen's three fields through main, which joins the hub
// with them if they differ. Resolves with them as stored, or with null when
// the team code is not valid.
export const saveSettings = async (typed: TypedSettings) => {
  const settings = await window.redlink.writeSettings(typed)
  if (settings) useHubStore.setState({ settings })
  return settings
}

// The stream fills the store the direct link fills.
window.redlink.onSample((sample) => {
  if (fillsStore()) useVehicleStore.setState(sample)
})

// The status line's text as a viewer, or null for none.
export const viewerStatus = ({ connection }: HubState) => {
  switch (connection.state) {
    case 'no_team_code':
      return 'Takım kodu girilmedi'
    case 'hub_unreachable':
      return "Hub'a ulaşılamıyor"
    case 'phone_not_found':
      return 'Telefon bulunamadı'
    case 'joining':
      return 'Bağlanılıyor'
    case 'live':
      return null
    case 'board_unreachable':
      return 'Araç yanıt vermiyor'
    case 'phone_lost':
      return 'Telefon bağlantısı koptu, yeniden bağlanılıyor'
    case 'version_mismatch':
      return `Telefon sürüm ${connection.phoneMajor}, bu uygulama sürüm ${connection.appMajor}`
  }
}

import { app, BrowserWindow, ipcMain } from 'electron'
import { join } from 'node:path'
import { handleBluetoothChooser } from './bluetooth'
import { handleCsvSave } from './csv'
import { startHubClient } from './hub-client'
import { connectToHub } from './hub-socket'
import { readSettings, saveSettings } from './settings'
import { appMajorVersion, appVersion } from './version'
import type { Settings, TypedSettings } from '../preload/api'

// This instance holds the settings it read on launch or last saved. A change
// saved in another window reaches this one when it is restarted.
const handleSettings = (file: string, launched: Settings, onSaved: (settings: Settings) => void) => {
  let settings = launched

  ipcMain.handle('settings:read', () => settings)
  ipcMain.handle('settings:write', async (_event, typed: TypedSettings) => {
    const stored = await saveSettings(file, typed)
    if (stored) {
      settings = stored
      onSaved(stored)
    }
    return stored
  })
}

// The hub client lives here and not in the renderer, whose timers a hidden
// window may slow, which would starve the heartbeat. With a team code stored
// it starts its first lobby visit now, with no click. The renderer is sent
// each sample and each change of the connection state, and reads the state
// once when it starts, having missed what was sent before. The renderer owns
// the direct link and says when one starts and ends: the app is a viewer or on
// a direct link, never both.
const handleHub = (window: BrowserWindow, settings: Settings) => {
  const send = (channel: string, value: unknown) => {
    if (!window.isDestroyed()) window.webContents.send(channel, value)
  }
  const client = startHubClient(
    { ...settings, majorVersion: appMajorVersion },
    {
      connect: connectToHub,
      clock: { now: Date.now, setTimeout, clearTimeout: (timer) => clearTimeout(timer as NodeJS.Timeout) },
      random: Math.random,
    },
    { state: (state) => send('hub:state', state), sample: (sample) => send('hub:sample', sample) },
  )
  ipcMain.handle('hub:state', () => client.state())
  ipcMain.on('direct-link:started', client.directLinkStarted)
  ipcMain.on('direct-link:ended', client.directLinkEnded)
  // A renderer that loads has no direct link. One reloaded in the middle of a
  // direct link never said it ended.
  window.webContents.on('did-finish-load', client.directLinkEnded)
  return client
}

const createWindow = (route: string) => {
  const window = new BrowserWindow({
    width: 1440,
    height: 900,
    title: 'redLINK',
    backgroundColor: '#000000',
    webPreferences: {
      preload: join(import.meta.dirname, '../preload/index.cjs'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
      // The direct link polls the board from the renderer every 50 ms, which
      // a hidden or minimised window would otherwise slow to once a second.
      backgroundThrottling: false,
    },
  })

  handleBluetoothChooser(window)

  if (!app.isPackaged && process.env.ELECTRON_RENDERER_URL) {
    window.loadURL(`${process.env.ELECTRON_RENDERER_URL}#${route}`)
  } else {
    window.loadFile(join(import.meta.dirname, '../renderer/index.html'), { hash: route })
  }
  return window
}

// No single-instance lock: a second start is a second, independent viewer.
app.whenReady().then(async () => {
  ipcMain.handle('app:version', () => appVersion)
  handleCsvSave()
  const file = join(app.getPath('userData'), 'settings.json')
  const settings = await readSettings(file)
  // With no team code stored the app opens on the settings screen.
  const window = createWindow(settings.teamCode ? '/' : '/settings')
  const hub = handleHub(window, settings)
  // Saving a different team code, host or port makes the hub client leave and
  // join again with the new values.
  handleSettings(file, settings, hub.settingsSaved)
})

// One window, and closing it quits the app on macOS as on Windows.
app.on('window-all-closed', () => app.quit())

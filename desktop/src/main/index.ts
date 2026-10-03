import { app, BrowserWindow, ipcMain } from 'electron'
import { join } from 'node:path'
import { handleBluetoothChooser } from './bluetooth'
import { handleCsvSave } from './csv'
import { readSettings, saveSettings } from './settings'
import { appVersion } from './version'
import type { TypedSettings } from '../preload/api'

// This instance holds the settings it read on launch or last saved. A change
// saved in another window reaches this one when it is restarted.
const handleSettings = async () => {
  const file = join(app.getPath('userData'), 'settings.json')
  let settings = await readSettings(file)

  ipcMain.handle('settings:read', () => settings)
  ipcMain.handle('settings:write', async (_event, typed: TypedSettings) => {
    const saved = await saveSettings(file, typed)
    if (saved) settings = saved
    return saved
  })
  return settings
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
}

// No single-instance lock: a second start is a second, independent viewer.
app.whenReady().then(async () => {
  ipcMain.handle('app:version', () => appVersion)
  handleCsvSave()
  const settings = await handleSettings()
  // With no team code stored the app opens on the settings screen.
  createWindow(settings.teamCode ? '/' : '/settings')
})

// One window, and closing it quits the app on macOS as on Windows.
app.on('window-all-closed', () => app.quit())

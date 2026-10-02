import { app, BrowserWindow, ipcMain } from 'electron'
import { join } from 'node:path'
import { appVersion } from './version'

const createWindow = () => {
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
    },
  })

  if (!app.isPackaged && process.env.ELECTRON_RENDERER_URL) {
    window.loadURL(process.env.ELECTRON_RENDERER_URL)
  } else {
    window.loadFile(join(import.meta.dirname, '../renderer/index.html'))
  }
}

// No single-instance lock: a second start is a second, independent viewer.
app.whenReady().then(() => {
  ipcMain.handle('app:version', () => appVersion)
  createWindow()
})

// One window, and closing it quits the app on macOS as on Windows.
app.on('window-all-closed', () => app.quit())

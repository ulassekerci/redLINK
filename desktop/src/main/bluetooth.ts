import { ipcMain, type BrowserWindow } from 'electron'
import type { BluetoothDeviceEntry } from '../preload/api'

// Electron has no Bluetooth chooser of its own. A scan started in the renderer
// raises select-bluetooth-device, again each time the list grows; main passes
// the list on and answers with the device the person picks, or with none on a
// cancel, which ends the scan. Nothing is picked automatically.
export function handleBluetoothChooser(window: BrowserWindow) {
  let answer: ((deviceId: string) => void) | null = null

  window.webContents.on('select-bluetooth-device', (event, devices, callback) => {
    event.preventDefault()
    answer = callback
    const entries: BluetoothDeviceEntry[] = devices.map((device) => ({ id: device.deviceId, name: device.deviceName }))
    window.webContents.send('bluetooth:devices', entries)
  })

  const reply = (deviceId: string) => {
    answer?.(deviceId)
    answer = null
  }

  ipcMain.on('bluetooth:pick', (event, deviceId: string) => {
    if (event.sender === window.webContents) reply(deviceId)
  })
  ipcMain.on('bluetooth:cancel', (event) => {
    if (event.sender === window.webContents) reply('')
  })
}

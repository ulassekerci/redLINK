import { contextBridge, ipcRenderer, type IpcRendererEvent } from 'electron'
import type { RedlinkApi } from './api'

// Subscribes a listener to what main sends on a channel, and returns a
// function that unsubscribes.
const subscribe =
  <T>(channel: string) =>
  (listener: (value: T) => void) => {
    const handler = (_event: IpcRendererEvent, value: T) => listener(value)
    ipcRenderer.on(channel, handler)
    return () => ipcRenderer.off(channel, handler)
  }

const api: RedlinkApi = {
  version: () => ipcRenderer.invoke('app:version'),
  onSample: subscribe('hub:sample'),
  onConnectionState: subscribe('hub:state'),
  readConnectionState: () => ipcRenderer.invoke('hub:state'),
  onBluetoothDevices: subscribe('bluetooth:devices'),
  pickBluetoothDevice: (id) => ipcRenderer.send('bluetooth:pick', id),
  cancelBluetoothScan: () => ipcRenderer.send('bluetooth:cancel'),
  directLinkStarted: () => ipcRenderer.send('direct-link:started'),
  directLinkEnded: () => ipcRenderer.send('direct-link:ended'),
  saveCsv: (defaultName, csv) => ipcRenderer.invoke('csv:save', defaultName, csv),
  readSettings: () => ipcRenderer.invoke('settings:read'),
  writeSettings: (typed) => ipcRenderer.invoke('settings:write', typed),
}

contextBridge.exposeInMainWorld('redlink', api)

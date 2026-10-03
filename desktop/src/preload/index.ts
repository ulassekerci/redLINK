import { contextBridge, ipcRenderer, type IpcRendererEvent } from 'electron'
import type { BluetoothDeviceEntry, RedlinkApi } from './api'

const api: RedlinkApi = {
  version: () => ipcRenderer.invoke('app:version'),
  onBluetoothDevices: (listener) => {
    const handler = (_event: IpcRendererEvent, devices: BluetoothDeviceEntry[]) => listener(devices)
    ipcRenderer.on('bluetooth:devices', handler)
    return () => ipcRenderer.off('bluetooth:devices', handler)
  },
  pickBluetoothDevice: (id) => ipcRenderer.send('bluetooth:pick', id),
  cancelBluetoothScan: () => ipcRenderer.send('bluetooth:cancel'),
  saveCsv: (defaultName, csv) => ipcRenderer.invoke('csv:save', defaultName, csv),
}

contextBridge.exposeInMainWorld('redlink', api)

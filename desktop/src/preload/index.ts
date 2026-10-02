import { contextBridge, ipcRenderer } from 'electron'
import type { RedlinkApi } from './api'

const api: RedlinkApi = {
  version: () => ipcRenderer.invoke('app:version'),
}

contextBridge.exposeInMainWorld('redlink', api)

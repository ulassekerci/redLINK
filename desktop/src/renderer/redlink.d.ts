import type { RedlinkApi } from '../preload/api'

declare global {
  interface Window {
    redlink: RedlinkApi
  }
}

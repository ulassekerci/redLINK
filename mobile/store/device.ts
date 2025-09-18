import AsyncStorage from '@react-native-async-storage/async-storage'
import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

export interface DeviceState {
  deviceName: string | null
  gpsPermissions: {
    fg: boolean | null
    bg: boolean | null
  }

  updateName: (deviceName: string) => void
  updateGPSPermission: {
    fg: (granted: boolean) => void
    bg: (granted: boolean) => void
  }
}

export const useDeviceStore = create<DeviceState>()(
  persist(
    (set) => {
      return {
        deviceName: null,
        gpsPermissions: { fg: null, bg: null },

        updateName: (deviceName) => set({ deviceName }),
        updateGPSPermission: {
          fg: (granted) => set((state) => ({ gpsPermissions: { ...state.gpsPermissions, fg: granted } })),
          bg: (granted) => set((state) => ({ gpsPermissions: { ...state.gpsPermissions, bg: granted } })),
        },
      }
    },
    {
      name: 'uiState',
      storage: createJSONStorage(() => AsyncStorage),
    }
  )
)

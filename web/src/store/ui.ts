import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface uiState {
  aston: boolean
  toggleAston: () => void
}

export const useUIStore = create<uiState>()(
  persist(
    (set) => ({
      aston: false,
      toggleAston: () => set((state) => ({ aston: !state.aston })),
    }),
    { name: 'uiState' }
  )
)

import { create } from 'zustand'

interface uiState {
  aston: boolean
  toggleAston: () => void
}

export const useUIStore = create<uiState>()((set) => ({
  aston: false,
  toggleAston: () => set((state) => ({ aston: !state.aston })),
}))

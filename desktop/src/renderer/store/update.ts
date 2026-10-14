import { create } from 'zustand'

interface UpdateState {
  // The newer release main's launch check found, or null for none.
  version: string | null
}

export const useUpdateStore = create<UpdateState>()(() => ({ version: null }))

window.redlink.update().then((version) => useUpdateStore.setState({ version }))

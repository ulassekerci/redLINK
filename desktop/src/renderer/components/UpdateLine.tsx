import { twMerge } from 'tailwind-merge'
import { useUpdateStore } from '../store/update'

// One line when a newer release exists, which opens its page in the browser.
export const UpdateLine = ({ className }: { className?: string }) => {
  const version = useUpdateStore((state) => state.version)
  if (!version) return null
  return (
    <span
      className={twMerge('cursor-pointer text-rose-100/80 hover:text-rose-500/80', className)}
      onClick={window.redlink.openUpdate}
    >
      Güncelleme var: {version}
    </span>
  )
}

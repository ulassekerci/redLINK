import { Warning } from '../components/Warning'
import { BottomSection } from '../components/BottomSection'
import { Outlet } from 'react-router'
import { useTripKeys } from '../hooks/useTripKeys'

export const Root = () => {
  useTripKeys()

  return (
    <div className='flex flex-col justify-between h-full'>
      <Warning />
      <Outlet />
      <BottomSection />
    </div>
  )
}

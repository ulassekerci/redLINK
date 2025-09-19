import { Warning } from '../components/Warning'
import { BottomSection } from '../components/BottomSection'
import { Outlet } from 'react-router'

export const Root = () => {
  return (
    <div className='flex flex-col justify-between h-full'>
      <Warning />
      <Outlet />
      <BottomSection />
    </div>
  )
}

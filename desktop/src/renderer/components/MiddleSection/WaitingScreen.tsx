import { PUBLIC_HUB, formatTeamCode } from '../../../protocol'
import { useHubStore } from '../../store/hub'
import { UpdateLine } from '../UpdateLine'
import { StatusLine } from './StatusLine'

// What the middle section shows under the logo while the app is not live: why
// it is waiting and the team code it is waiting with, so a laptop set up with
// another phone's code, or left on another hub, can be spotted. The update
// line is here as well as in settings, which the pit crew does not open after
// the first launch.
export const WaitingScreen = () => {
  const settings = useHubStore((state) => state.settings)
  const otherHub = settings && (settings.hubHost !== PUBLIC_HUB.host || settings.hubPort !== PUBLIC_HUB.port)

  return (
    <div className='absolute top-24 flex flex-col items-center w-full'>
      <StatusLine />
      {settings?.teamCode && <span className='text-gray-300/60'>Takım kodu: {formatTeamCode(settings.teamCode)}</span>}
      {otherHub && (
        <span className='pt-2 text-base text-gray-300/60'>
          {settings.hubHost}:{settings.hubPort}
        </span>
      )}
      <UpdateLine className='pt-4 text-base' />
    </div>
  )
}

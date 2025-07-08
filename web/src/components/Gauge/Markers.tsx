import { twMerge } from 'tailwind-merge'
import { range } from '../../utils'

export const GaugeMarkers = ({ max, step = 1 }: { max: number; step?: number }) => {
  return (
    <>
      {range(0, max).map((n) => {
        return (
          <div
            className='absolute text-center'
            style={{
              transform: `rotate(${n * (270 / max) - 135}deg)`,
              inset: n % (step * 10) === 0 ? '16px' : '12px',
            }}
            key={n + 'marker'}
          >
            <p
              className={twMerge(
                'w-0.5 inline-block bg-rose-300/50 h-2',
                n % (step * 10) === 0 && 'bg-rose-300 h-3 w-[3px]',
                n % step !== 0 && 'hidden'
              )}
            />
          </div>
        )
      })}
    </>
  )
}

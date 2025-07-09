import { twMerge } from 'tailwind-merge'
import { range } from '../../utils/range'

export const GaugeNumbers = ({ value, max, step = 10 }: { value: number; max: number; step?: number }) => {
  return (
    <>
      {range(0, max).map((n) => {
        return (
          <div
            className='absolute text-center'
            style={{
              transform: `rotate(${n * (270 / max) - 135}deg)`,
              inset: '48px',
            }}
            key={n + 'number'}
          >
            <p
              className={twMerge('inline-block text-rose-100/90 text-xl font-semibold', n % step !== 0 && 'hidden')}
              style={{
                transform: `rotate(${135 - (n / max) * 270}deg)`,
              }}
            >
              {n === max && value > max ? value : n}
            </p>
          </div>
        )
      })}
    </>
  )
}
